package com.example.toget.domain.user.entity;

import com.example.toget.domain.user.enums.OAuthProvider;
import com.example.toget.domain.user.enums.UserStatus;
import com.example.toget.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "profile_image_url", length = 512)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    /** Refresh Token Rotation — 현재 유효한 refresh 토큰의 jti(UUID) 저장 */
    @Column(name = "refresh_token", length = 36)
    private String refreshToken;

    // @Builder를 생성자에 붙이면 빌더가 이 파라미터들만 받는다.
    // id(자동 생성), status(아래에서 고정), refreshToken(로그인 시 별도 설정)은 빌더에서 제외됨.
    @Builder
    private User(OAuthProvider oAuthProvider, String oAuthId, String email, String name,
                 String nickname, String profileImageUrl) {
        this.oAuthProvider = oAuthProvider;
        this.oAuthId = oAuthId;
        this.email = email;
        this.name = name;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.status = UserStatus.ACTIVE; // 신규 가입자는 항상 활성 상태로 시작
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

    /** Soft Delete — 물리 삭제 대신 상태를 WITHDRAWN으로 전환 + 세션(refresh token) 만료 */
    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
        this.refreshToken = null;
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
