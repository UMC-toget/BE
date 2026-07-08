package com.example.toget.domain.user.service;

import com.example.toget.domain.user.converter.UserConverter;
import com.example.toget.domain.user.dto.OAuthUserInfo;
import com.example.toget.domain.user.dto.SocialLoginResponse;
import com.example.toget.domain.user.dto.TokenResponse;
import com.example.toget.domain.user.entity.User;
import com.example.toget.domain.user.enums.OAuthProvider;
import com.example.toget.domain.user.exception.UserErrorCode;
import com.example.toget.domain.user.exception.UserException;
import com.example.toget.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 인증(로그인/토큰 재발급) 비즈니스 로직.
 *
 * [소셜 로그인 전체 흐름]
 *  1. 프론트가 카카오/구글 SDK로 로그인하고 identityToken(카카오는 access token, 구글은 ID token)을 받아옴
 *  2. 프론트 → 우리 서버 POST /api/v1/auth/tokens/{provider} 로 그 토큰을 전달
 *  3. 서버는 OAuthClient로 공급자 서버에 "이 토큰 진짜야?"라고 검증 요청
 *  4. 진짜면 우리 DB에서 사용자를 찾고(없으면 자동 회원가입) 우리 서비스 전용 JWT를 발급
 *  → 이후 API 호출은 공급자와 무관하게 우리 JWT로만 인증한다
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    // 같은 인터페이스(OAuthClient)의 빈이 여러 개면 스프링이 List로 전부 모아서 주입해 준다.
    // 새 공급자가 생겨도 OAuthClient 구현체만 추가하면 이 클래스는 수정할 필요가 없다.
    private final List<OAuthClient> oauthClients;

    /**
     * 소셜 로그인 및 자동 회원가입.
     * 미가입 oauth_id면 users에 새 레코드를 적재하고 isNewUser=true를 반환한다.
     */
    @Transactional // 메서드 전체가 하나의 DB 트랜잭션 — 도중 예외 시 롤백, 정상 종료 시 커밋
    public SocialLoginResponse socialLogin(String providerName, String identityToken) {
        OAuthProvider provider = OAuthProvider.from(providerName);
        // 주입받은 클라이언트 목록에서 이 공급자를 담당하는 구현체를 찾는다
        OAuthClient client = oauthClients.stream()
                .filter(c -> c.provider() == provider)
                .findFirst()
                .orElseThrow(() -> new UserException(UserErrorCode.UNSUPPORTED_PROVIDER));
        OAuthUserInfo info = client.verify(identityToken); // 공급자 서버에 실검증 (위조 토큰이면 여기서 401)

        var existing = userRepository.findByOAuthProviderAndOAuthId(provider, info.oAuthId());
        boolean isNewUser = existing.isEmpty();
        // orElseGet: Optional이 비어 있을 때만 람다 실행 → 기존 회원이면 조회 결과, 신규면 저장 후 반환
        // (동시에 같은 계정이 첫 로그인하면 유니크 제약 위반 → GeneralExceptionAdvice가 409로 변환)
        User user = existing
                .orElseGet(() -> userRepository.save(User.builder()
                        .oAuthProvider(provider)
                        .oAuthId(info.oAuthId())
                        .email(info.email())
                        .name(info.name())
                        .nickname(info.name()) // 초기 닉네임은 소셜 프로필 이름으로
                        .profileImageUrl(info.profileImageUrl())
                        .build()));

        // 탈퇴한 계정은 재로그인 시 인증 오류로 처리
        if (!user.isActive()) {
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }

        TokenResponse tokens = issueTokens(user);
        return UserConverter.toSocialLoginResponse(user, tokens.accessToken(), tokens.refreshToken(), isNewUser);
    }

    /**
     * Refresh Token Rotation — users.refresh_token에 저장된 jti와 대조한다.
     *
     * [Rotation이란] 재발급할 때마다 refresh token도 새것으로 갈아끼우고 DB에는 최신 jti만 남긴다.
     * 그래서 이미 교체된(=구버전) refresh token이 다시 들어오면 "누군가 훔쳐서 재사용 중"으로 판단할 수 있다.
     * 재사용이 감지되면 저장된 refresh 토큰까지 무효화해 세션을 강제 만료시킨다.
     *
     * (noRollbackFor: 예외를 던지면 @Transactional이 기본적으로 롤백하는데,
     *  그러면 방금 수행한 무효화(clearRefreshToken)까지 되돌아가 버리므로 UserException은 롤백 대상에서 제외)
     * 만료 검증은 JWT exp 클레임(jwtProvider.parse)으로 수행한다.
     */
    @Transactional(noRollbackFor = UserException.class)
    public TokenResponse refresh(String refreshTokenValue) {
        JwtClaims claims = jwtProvider.parse(refreshTokenValue, JwtProvider.TOKEN_TYPE_REFRESH);
        Long userId = jwtProvider.getUserId(claims);

        // 존재하지 않는 사용자도 401로 응답 — 404를 주면 "이 ID는 존재한다/안 한다"가 노출되기 때문
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.UNAUTHORIZED));
        if (!user.isActive()) {
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }

        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(claims.id())) {
            user.clearRefreshToken(); // 재사용 감지 → 세션 강제 로그아웃
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }

        return issueTokens(user);
    }

    /** 토큰 발급 — 새 refresh 토큰의 jti를 users.refresh_token에 저장(Rotation) */
    private TokenResponse issueTokens(User user) {
        String tokenId = UUID.randomUUID().toString();
        // 트랜잭션 안에서 엔티티 필드만 바꾸면 커밋 시점에 JPA가 변경을 감지(dirty checking)해
        // UPDATE 쿼리를 자동 실행한다 — save()를 다시 부를 필요가 없다
        user.updateRefreshToken(tokenId);
        return new TokenResponse(
                jwtProvider.createAccessToken(user.getId()),
                jwtProvider.createRefreshToken(user.getId(), tokenId)
        );
    }
}
