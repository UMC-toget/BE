package com.example.toget.domain.user.entity;

import com.example.toget.domain.user.enums.OAuthProvider;
import com.example.toget.domain.user.enums.UserStatus;
import com.example.toget.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 회원 엔티티 — users 테이블과 1:1로 매핑되는 JPA 엔티티.
 *
 * [설계 포인트]
 *  - 소셜 로그인 전용이라 비밀번호 컬럼이 없다. (oauth_provider + oauth_id) 조합이 로그인 식별자.
 *  - setter를 만들지 않고 updateProfile()/withdraw() 같은 의도가 드러나는 메서드로만 상태를 바꾼다.
 *    → 아무 데서나 값이 바뀌는 것을 막고, 변경 규칙을 엔티티 안에 모은다.
 *  - createdAt/updatedAt은 BaseEntity + @EnableJpaAuditing이 자동으로 채운다.
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        // 같은 소셜 계정으로 중복 가입되지 않도록 DB 차원에서 강제하는 복합 유니크 제약
        @UniqueConstraint(name = "uk_users_oauth", columnNames = {"oauth_provider", "oauth_id"})
})
@Getter
// JPA는 DB에서 읽은 값을 채워 넣기 위해 기본 생성자가 필요하다.
// 단 외부 코드가 new User()로 빈 객체를 만들지 못하게 protected로 제한.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL AUTO_INCREMENT에 ID 생성을 위임
    @Column(name = "user_id")
    private Long id;

    // 열거형을 숫자(ORDINAL)가 아닌 이름 문자열("KAKAO")로 저장 —
    // 상수 순서가 바뀌어도 기존 데이터가 깨지지 않아 항상 STRING을 쓴다
    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider", nullable = false, length = 20)
    @Getter(AccessLevel.NONE)
    private OAuthProvider oAuthProvider;

    /** 공급자가 발급한 사용자 고유 ID (카카오 회원번호, 구글 sub) */
    @Column(name = "oauth_id", nullable = false, length = 255)
    @Getter(AccessLevel.NONE)
    private String oAuthId;

    @Column(length = 320) // 이메일 최대 길이 표준(로컬 64 + @ + 도메인 255)
    private String email;

    @Column(length = 100)
    private String name;

    @Column(length = 50)
    private String nickname;

    // 웹 사진 검색(issue #102)으로 고른 외부 이미지 URL은 쿼리스트링 때문에 512자를 넘길 수 있어
    // TEXT로 확장한다. gift_image_url / wishlist.image_url과 동일한 정책.
    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    /** Refresh Token Rotation — 현재 유효한 refresh 토큰의 jti(UUID) 저장 */
    @Column(name = "refresh_token", length = 36)
    private String refreshToken;

    // @Builder를 생성자에 붙이면 빌더가 이 파라미터들만 받는다.
    // id(자동 생성), status(아래에서 고정), refreshToken(로그인 시 별도 설정)은 빌더에서 제외됨.
    //
    // [중요] 이 생성자가 호출되는 시점 = 회원가입이 확정되는 시점이다.
    // 소셜 인증만 끝난 사람은 users 레코드를 만들지 않고 가입 토큰만 발급받으며(JwtProvider 참고),
    // 프로필 설정을 마쳐야 비로소 여기까지 온다. 그래서 nickname은 항상 채워져 있고 status는 ACTIVE다.
    // 과거에는 소셜 인증 직후 바로 생성해서, 프로필 설정 화면에서 이탈해도 "소셜 이름 + 기본 프로필"로
    // 계정이 확정되고 재로그인 시 온보딩이 다시 뜨지 않는 문제가 있었다. (issue #61)
    @Builder
    private User(OAuthProvider oAuthProvider, String oAuthId, String email, String name,
                 String nickname, String profileImageUrl) {
        this.oAuthProvider = oAuthProvider;
        this.oAuthId = oAuthId;
        this.email = email;
        this.name = name;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.status = UserStatus.ACTIVE; // 프로필 설정까지 마치고 생성되므로 곧바로 활성 상태
    }

    /** null인 필드는 건드리지 않는 부분 수정(PATCH) 방식. 공백 문자열("") 입력 시 프로필 이미지 초기화 */
    public void updateProfile(String nickname, String profileImageUrl) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl.isEmpty() ? null : profileImageUrl;
        }
    }

    public OAuthProvider getOAuthProvider() {
        return this.oAuthProvider;
    }

    public String getOAuthId() {
        return this.oAuthId;
    }

    /** 프로필 이미지 초기화 (기본 이미지 사용) */
    public void clearProfileImage() {
        this.profileImageUrl = null;
    }

    /**
     * 이 회원이 설정된 관리자 계정인지 판정한다.
     *
     * <p>공급자와 이메일을 <b>모두</b> 비교하는 이유: 이메일만 보면 관리자 이메일 주소를
     * 다른 소셜 계정(예: 카카오)에 등록한 뒤 로그인해 관리자 권한을 얻는 우회가 가능하다.
     *
     * <p>탈퇴 회원은 withdraw()에서 email이 null이 되므로 자연히 false가 된다.
     * (애초에 ActiveUserReader가 탈퇴 회원을 먼저 401로 차단한다)
     *
     * <p>향후 관리자가 여러 명이 되거나 등급 구분이 필요해지면 role 컬럼 기반 판정으로
     * 이 메서드의 내부만 교체하면 된다.
     */
    public boolean isAdmin(OAuthProvider adminProvider, String adminEmail) {
        if (adminProvider == null || adminEmail == null || adminEmail.isBlank()) {
            return false;
        }
        // adminEmail을 수신자로 두어 this.email이 null이어도 NPE가 나지 않는다
        return this.oAuthProvider == adminProvider && adminEmail.equalsIgnoreCase(this.email);
    }

    /**
     * Soft Delete + 개인정보 익명화 — 상태를 WITHDRAWN으로 전환하고 세션(refresh token)을 만료시킨다.
     * oauth_id를 무작위 값으로 교체하므로 (oauth_provider, oauth_id) 유니크 제약에서 벗어나고,
     * 같은 소셜 계정으로 다시 로그인하면 완전히 새 계정으로 가입된다.
     * 레코드 자체는 남기므로 정산 이력 등 연관 데이터의 FK는 깨지지 않는다.
     */
    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
        this.refreshToken = null;
        this.oAuthId = "withdrawn:" + UUID.randomUUID(); // 원본 소셜 식별자 파기
        this.email = null;
        this.name = null;
        this.nickname = null;
        this.profileImageUrl = null;
    }

    /** 활성 사용자 판정 — 탈퇴 여부는 status(WITHDRAWN)로 판단한다 */
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    /** Rotation — 새로 발급한 refresh 토큰의 jti로 교체 */
    public void updateRefreshToken(String refreshTokenId) {
        this.refreshToken = refreshTokenId;
    }

    /** 세션 강제 만료 — 저장된 refresh 토큰 무효화 */
    public void clearRefreshToken() {
        this.refreshToken = null;
    }
}
