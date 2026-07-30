package com.example.toget.domain.user.service;

import com.example.toget.domain.user.converter.UserConverter;
import com.example.toget.domain.user.dto.SignupCompleteRequest;
import com.example.toget.domain.user.dto.SignupCompleteResponse;
import com.example.toget.domain.user.dto.TokenResponse;
import com.example.toget.domain.user.entity.User;
import com.example.toget.domain.user.enums.OAuthProvider;
import com.example.toget.domain.user.exception.UserErrorCode;
import com.example.toget.domain.user.exception.UserException;
import com.example.toget.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 완료 처리 — 프로필(닉네임/이미지) 설정과 동시에 users 레코드를 만든다.
 *
 * <p>[이 클래스가 생긴 이유] 예전에는 소셜 인증만 끝나도 AuthService가 곧바로 회원을 저장해
 * 가입이 확정됐다. 그래서 프로필 설정 화면에서 새로고침·이탈하면 "소셜 이름 + 기본 프로필"로
 * 계정이 남고, 재로그인해도 온보딩이 뜨지 않아 닉네임이 고정됐다.
 * 이제 <b>회원이 만들어지는 지점은 오직 여기 한 곳</b>이며, 소셜 인증 결과는 그때까지
 * 서명된 가입 토큰(signup token) 형태로 클라이언트가 들고 있는다. (issue #61)
 */
@Service
@RequiredArgsConstructor
public class SignupService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final TokenIssuer tokenIssuer;

    @Transactional
    public SignupCompleteResponse completeSignup(SignupCompleteRequest request) {
        // 위조·만료된 토큰은 여기서 401. 소셜 식별자는 반드시 서명된 토큰에서만 꺼낸다 —
        // 요청 본문으로 받으면 남의 소셜 계정으로 가입하는 것을 막을 수 없다.
        SignupClaims claims = jwtProvider.parseSignupToken(request.signupToken());
        OAuthProvider provider = OAuthProvider.from(claims.provider());

        // 이미 가입된 소셜 계정인지 먼저 확인 — 뒤로가기·중복 제출로 같은 가입 토큰이
        // 두 번 들어오는 경우가 실제로 흔하다. (아래 유니크 제약이 최종 방어선)
        if (userRepository.findByOAuthProviderAndOAuthId(provider, claims.oAuthId()).isPresent()) {
            throw new UserException(UserErrorCode.ALREADY_REGISTERED);
        }

        User user = User.builder()
                .oAuthProvider(provider)
                .oAuthId(claims.oAuthId())
                .email(claims.email())
                .name(claims.name())
                .nickname(request.nickname())
                // 미설정(null)이면 소셜 프로필 이미지를 기본값으로 쓴다
                .profileImageUrl(request.profileImageUrl() != null
                        ? request.profileImageUrl() : claims.profileImageUrl())
                .build();

        try {
            // saveAndFlush로 INSERT를 지금 실행한다 — 커밋 시점까지 미루면 유니크 제약 위반이
            // 트랜잭션 밖에서 터져 아래 catch로 잡히지 않고 500이 나간다.
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // 같은 가입 토큰으로 동시에 두 번 요청한 경우 — 위 조회는 둘 다 통과할 수 있으므로
            // (oauth_provider, oauth_id) 유니크 제약이 실제로 중복을 막는다
            throw new UserException(UserErrorCode.ALREADY_REGISTERED);
        }

        // 가입이 확정된 이 시점에 비로소 서비스 토큰을 발급한다
        TokenResponse tokens = tokenIssuer.issue(user);
        return UserConverter.toSignupCompleteResponse(user, tokens.accessToken(), tokens.refreshToken());
    }
}
