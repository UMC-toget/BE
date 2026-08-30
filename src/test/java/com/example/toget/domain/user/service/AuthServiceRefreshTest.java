package com.example.toget.domain.user.service;

import com.example.toget.domain.user.dto.TokenResponse;
import com.example.toget.domain.user.entity.User;
import com.example.toget.domain.user.enums.OAuthProvider;
import com.example.toget.domain.user.exception.UserErrorCode;
import com.example.toget.domain.user.exception.UserException;
import com.example.toget.domain.user.repository.UserRepository;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * 토큰 재발급(Refresh Token Rotation) 테스트.
 *
 * <p><b>핵심 계약: 구버전 refresh token이 들어와도 저장된 세션(users.refresh_token)은 건드리지 않는다.</b>
 *
 * <p>[배경] 예전에는 jti 불일치를 "탈취 후 재사용"으로 보고 저장된 jti까지 지웠다.
 * 그런데 jti를 하나만 보관하는 구조라, 한 계정을 기기 두 대에서 쓰기만 해도
 * 오래된 기기의 재발급 시도가 <b>멀쩡히 쓰고 있는 다른 기기의 세션까지</b> 끊어버렸다.
 * access token 수명이 1시간이라 하루 안에 반드시 재현되는 문제였다.
 * 이 테스트는 그 회귀를 막는다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceRefreshTest {

    private static final Long USER_ID = 1L;
    private static final String CURRENT_JTI = "current-jti";
    private static final String STALE_JTI = "stale-jti";
    private static final String REFRESH_TOKEN = "refresh-token-value";

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private TokenIssuer tokenIssuer;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        // oauthClients가 List로 주입되는 구조라 @InjectMocks 대신 직접 조립한다
        authService = new AuthService(userRepository, jwtProvider, tokenIssuer, List.of());
    }

    /** 저장된 jti를 지정한 회원 (영속 엔티티를 흉내 내기 위해 id 주입) */
    private User userWithJti(String storedJti) {
        User user = User.builder()
                .oAuthProvider(OAuthProvider.KAKAO)
                .oAuthId("kakao-1")
                .email("user@example.com")
                .name("홍길동")
                .nickname("투겟러브")
                .build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        if (storedJti != null) {
            user.updateRefreshToken(storedJti);
        }
        return user;
    }

    /** 요청으로 들어온 refresh token이 주어진 jti를 담고 있도록 파싱 결과를 세팅 */
    private void givenIncomingJti(String jti) {
        JwtClaims claims = new JwtClaims(USER_ID, jti, JwtProvider.TOKEN_TYPE_REFRESH);
        given(jwtProvider.parse(REFRESH_TOKEN, JwtProvider.TOKEN_TYPE_REFRESH)).willReturn(claims);
        given(jwtProvider.getUserId(claims)).willReturn(USER_ID);
    }

    private static void assertRejectedWith(UserErrorCode expected, ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(UserException.class)
                .satisfies(e -> assertThat(((UserException) e).getCode()).isEqualTo(expected));
    }

    @Test
    @DisplayName("저장된 jti와 일치하면 새 토큰을 발급한다")
    void issuesNewTokensWhenJtiMatches() {
        User user = userWithJti(CURRENT_JTI);
        givenIncomingJti(CURRENT_JTI);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(tokenIssuer.issue(user)).willReturn(new TokenResponse("new-access", "new-refresh"));

        TokenResponse tokens = authService.refresh(REFRESH_TOKEN);

        assertThat(tokens.accessToken()).isEqualTo("new-access");
        assertThat(tokens.refreshToken()).isEqualTo("new-refresh");
    }

    @Test
    @DisplayName("구버전 jti로 재발급을 시도해도 저장된 세션은 그대로 유지된다 (다른 기기 보호)")
    void doesNotWipeSessionWhenStaleJtiArrives() {
        // 다른 기기가 이미 로그인해 저장된 jti는 CURRENT_JTI로 교체된 상태.
        // 오래된 기기가 STALE_JTI를 들고 재발급을 시도한다.
        User user = userWithJti(CURRENT_JTI);
        givenIncomingJti(STALE_JTI);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        assertRejectedWith(UserErrorCode.UNAUTHORIZED, () -> authService.refresh(REFRESH_TOKEN));

        // 이 단정이 이 클래스의 존재 이유다 — 예전 구현은 여기서 null이 되어
        // 정상 사용 중이던 다른 기기까지 로그아웃시켰다.
        assertThat(user.getRefreshToken()).isEqualTo(CURRENT_JTI);
    }

    @Test
    @DisplayName("로그아웃 상태(저장된 jti 없음)에서의 재발급은 401로 거부한다")
    void rejectsWhenNoStoredJti() {
        User user = userWithJti(null);
        givenIncomingJti(STALE_JTI);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        assertRejectedWith(UserErrorCode.UNAUTHORIZED, () -> authService.refresh(REFRESH_TOKEN));

        assertThat(user.getRefreshToken()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 404가 아니라 401로 응답한다 (계정 존재 여부 노출 방지)")
    void rejectsUnknownUserWithUnauthorized() {
        givenIncomingJti(CURRENT_JTI);
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        assertRejectedWith(UserErrorCode.UNAUTHORIZED, () -> authService.refresh(REFRESH_TOKEN));
    }

    @Test
    @DisplayName("비활성 상태의 회원은 jti가 일치해도 재발급하지 않는다")
    void rejectsInactiveUser() {
        User user = userWithJti(CURRENT_JTI);
        user.withdraw(); // status를 ACTIVE가 아닌 값으로 전환
        givenIncomingJti(CURRENT_JTI);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

        assertRejectedWith(UserErrorCode.UNAUTHORIZED, () -> authService.refresh(REFRESH_TOKEN));
    }
}
