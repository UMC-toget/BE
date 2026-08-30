package com.example.toget.domain.user.service;

import com.example.toget.domain.user.dto.OAuthUserInfo;
import com.example.toget.domain.user.dto.SocialLoginResponse;
import com.example.toget.domain.user.dto.TokenResponse;
import com.example.toget.domain.user.entity.User;
import com.example.toget.domain.user.enums.OAuthProvider;
import com.example.toget.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 소셜 로그인의 가입 여부 분기 테스트 (issue #61).
 *
 * 핵심 계약: <b>미가입자는 users 레코드를 만들지 않는다.</b>
 * 프로필 설정 화면에서 새로고침·이탈해도 계정이 남지 않아야 하고,
 * 재로그인하면 매번 다시 가입 토큰만 받아 온보딩으로 유도되어야 한다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceSignupStatusTest {

    private static final String IDENTITY_TOKEN = "kakao-access-token";
    private static final Long USER_ID = 1L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private TokenIssuer tokenIssuer;

    @Mock
    private OAuthClient kakaoClient;

    private AuthService authService;

    private final OAuthUserInfo userInfo =
            new OAuthUserInfo("kakao-1", "user@example.com", "홍길동", "https://toget.com/images/kakao.png");

    @BeforeEach
    void setUp() {
        // oauthClients가 List로 주입되는 구조라 @InjectMocks 대신 직접 조립한다
        authService = new AuthService(userRepository, jwtProvider, tokenIssuer, List.of(kakaoClient));
        given(kakaoClient.provider()).willReturn(OAuthProvider.KAKAO);
        given(kakaoClient.verify(IDENTITY_TOKEN)).willReturn(userInfo);
    }

    /** 가입까지 마친 기존 회원 (영속 엔티티를 흉내 내기 위해 id 주입) */
    private User registeredUser() {
        User user = User.builder()
                .oAuthProvider(OAuthProvider.KAKAO)
                .oAuthId("kakao-1")
                .email("user@example.com")
                .name("홍길동")
                .nickname("투겟러브")
                .profileImageUrl("https://toget.com/images/kakao.png")
                .build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    @Test
    @DisplayName("미가입자는 users에 저장하지 않고 가입 토큰만 발급한다")
    void unregistered_doesNotPersistUser() {
        given(userRepository.findByOAuthProviderAndOAuthId(OAuthProvider.KAKAO, "kakao-1"))
                .willReturn(Optional.empty());
        given(jwtProvider.createSignupToken(any(SignupClaims.class))).willReturn("signup-token");

        SocialLoginResponse response = authService.socialLogin("kakao", IDENTITY_TOKEN);

        // 이 검증이 이번 이슈의 핵심 — 저장되는 순간 "이탈해도 가입됨" 문제가 되살아난다
        verify(userRepository, never()).save(any());
        // 회원이 아니므로 서비스 토큰도 발급되지 않는다
        verify(tokenIssuer, never()).issue(any());

        assertThat(response.isProfileCompleted()).isFalse();
        assertThat(response.signupToken()).isEqualTo("signup-token");
        assertThat(response.accessToken()).isNull();
        assertThat(response.refreshToken()).isNull();
        assertThat(response.userId()).isNull();
        // 온보딩 화면이 소셜 이름을 미리 보여줄 수 있도록 프로필 정보는 함께 내려준다
        assertThat(response.name()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("가입 토큰에는 클라이언트가 위조할 수 없도록 소셜 식별자와 프로필 정보가 담긴다")
    void unregistered_signupTokenCarriesSocialIdentity() {
        given(userRepository.findByOAuthProviderAndOAuthId(OAuthProvider.KAKAO, "kakao-1"))
                .willReturn(Optional.empty());
        given(jwtProvider.createSignupToken(any(SignupClaims.class))).willReturn("signup-token");

        authService.socialLogin("kakao", IDENTITY_TOKEN);

        ArgumentCaptor<SignupClaims> captor = ArgumentCaptor.forClass(SignupClaims.class);
        verify(jwtProvider).createSignupToken(captor.capture());
        SignupClaims claims = captor.getValue();

        assertThat(claims.provider()).isEqualTo("KAKAO");
        assertThat(claims.oAuthId()).isEqualTo("kakao-1");
        assertThat(claims.email()).isEqualTo("user@example.com");
        assertThat(claims.name()).isEqualTo("홍길동");
        assertThat(claims.profileImageUrl()).isEqualTo("https://toget.com/images/kakao.png");
    }

    @Test
    @DisplayName("이탈 후 재로그인해도 여전히 미가입 상태라 가입 토큰만 다시 발급된다")
    void unregistered_reloginStillRequiresSignup() {
        // 첫 로그인에서 저장하지 않았으므로 재로그인 시에도 조회 결과가 비어 있다
        given(userRepository.findByOAuthProviderAndOAuthId(OAuthProvider.KAKAO, "kakao-1"))
                .willReturn(Optional.empty());
        given(jwtProvider.createSignupToken(any(SignupClaims.class))).willReturn("signup-token-2");

        SocialLoginResponse response = authService.socialLogin("kakao", IDENTITY_TOKEN);

        // 이 값이 true가 되면 프론트가 온보딩을 건너뛰어 닉네임이 고정된다 (issue #61의 원래 버그)
        assertThat(response.isProfileCompleted()).isFalse();
        assertThat(response.signupToken()).isEqualTo("signup-token-2");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("기존 회원은 가입 토큰 없이 access/refresh 토큰을 받는다")
    void registered_receivesServiceTokens() {
        given(userRepository.findByOAuthProviderAndOAuthId(OAuthProvider.KAKAO, "kakao-1"))
                .willReturn(Optional.of(registeredUser()));
        given(tokenIssuer.issue(any(User.class))).willReturn(new TokenResponse("access", "refresh"));

        SocialLoginResponse response = authService.socialLogin("kakao", IDENTITY_TOKEN);

        assertThat(response.isProfileCompleted()).isTrue();
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.signupToken()).isNull();
        // 기존 회원에게 가입 토큰을 주면 중복 가입 시도로 이어진다
        verify(jwtProvider, never()).createSignupToken(any());
    }
}
