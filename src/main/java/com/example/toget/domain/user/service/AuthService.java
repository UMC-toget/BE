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

/**
 * 인증(로그인/토큰 재발급) 비즈니스 로직.
 *
 * [소셜 로그인 전체 흐름]
 *  1. 프론트가 카카오/구글 SDK로 로그인하고 identityToken(카카오는 access token, 구글은 ID token)을 받아옴
 *  2. 프론트 → 우리 서버 POST /api/v1/auth/tokens/{provider} 로 그 토큰을 전달
 *  3. 서버는 OAuthClient로 공급자 서버에 "이 토큰 진짜야?"라고 검증 요청
 *  4. 진짜면 우리 DB에서 사용자를 찾는다
 *     - 기존 회원 → 우리 서비스 전용 JWT(access/refresh) 발급
 *     - 미가입자 → users에 저장하지 않고 가입 토큰만 발급.
 *       POST /api/v1/users(SignupService)에서 닉네임과 함께 보내야 비로소 회원이 된다. (issue #61)
 *  → 이후 API 호출은 공급자와 무관하게 우리 JWT로만 인증한다
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final TokenIssuer tokenIssuer;
    // 같은 인터페이스(OAuthClient)의 빈이 여러 개면 스프링이 List로 전부 모아서 주입해 준다.
    // 새 공급자가 생겨도 OAuthClient 구현체만 추가하면 이 클래스는 수정할 필요가 없다.
    private final List<OAuthClient> oauthClients;

    /**
     * 소셜 로그인 — 이미 가입한 회원인지에 따라 응답이 갈린다.
     *
     * <ul>
     *   <li><b>기존 회원</b>: access/refresh 토큰을 발급하고 isProfileCompleted=true로 응답한다.</li>
     *   <li><b>미가입자</b>: <b>users 레코드를 만들지 않고</b> 가입 토큰만 발급하며
     *       isProfileCompleted=false로 응답한다. 프론트는 이 값을 보고 프로필 설정 화면으로 보내고,
     *       {@code POST /api/v1/users}에서 가입 토큰과 닉네임을 함께 보내면 그때 회원이 생성된다.</li>
     * </ul>
     *
     * <p>[왜 미가입자를 저장하지 않는가] 예전에는 소셜 인증만 끝나도 곧바로 users에 적재해
     * 가입이 확정됐다. 그래서 프로필 설정 화면에서 새로고침하거나 이탈하면 "소셜 이름 + 기본 프로필"로
     * 계정이 남고, 재로그인해도 온보딩이 뜨지 않아 닉네임이 고정됐다. 아예 저장하지 않으면
     * 이탈한 사람의 흔적(이메일·이름 등 개인정보 포함)이 DB에 남지 않는다. (issue #61)
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

        User user = userRepository.findByOAuthProviderAndOAuthId(provider, info.oAuthId())
                .orElse(null);

        if (user == null) {
            // 아직 회원이 아니다 — 소셜 인증 결과를 서명된 가입 토큰에 담아 클라이언트에 넘긴다.
            // 서명이 있으므로 클라이언트가 남의 소셜 식별자로 바꿔치기할 수 없다.
            String signupToken = jwtProvider.createSignupToken(new SignupClaims(
                    provider.name(), info.oAuthId(), info.email(), info.name(), info.profileImageUrl()));
            return UserConverter.toSignupRequiredResponse(info, signupToken);
        }

        // 안전망 — 탈퇴 시 oauth_id가 익명화되므로 WITHDRAWN 계정이 여기서 조회될 일은 없지만,
        // 익명화 이전 데이터나 SUSPENDED 상태를 대비해 활성 상태를 한 번 더 확인한다
        if (!user.isActive()) {
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }

        TokenResponse tokens = tokenIssuer.issue(user);
        return UserConverter.toSocialLoginResponse(user, tokens.accessToken(), tokens.refreshToken());
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

        return tokenIssuer.issue(user);
    }

    /**
     * 로그아웃 — DB에 저장된 refresh 토큰(jti)을 제거해 이후 토큰 재발급을 차단한다.
     * 이미 로그아웃된(refresh_token == null) 상태로 다시 호출돼도 그대로 성공 처리한다(멱등).
     */
    @Transactional
    public void logout(Long userId) {
        // @LoginUserId로 넘어온, 이미 인증된 사용자다. 존재하지 않으면 refresh()와 동일하게 401.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.UNAUTHORIZED));
        // 트랜잭션 안에서 필드만 비우면 커밋 시 dirty checking으로 UPDATE가 자동 실행된다.
        user.clearRefreshToken();
    }
}
