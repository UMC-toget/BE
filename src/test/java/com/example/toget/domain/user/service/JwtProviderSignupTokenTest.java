package com.example.toget.domain.user.service;

import com.example.toget.domain.user.exception.UserErrorCode;
import com.example.toget.domain.user.exception.UserException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 가입 토큰(signup token) 발급·검증 테스트 (issue #61).
 *
 * mock 없이 실제 서명/검증을 돌린다 — 이 토큰은 "아직 회원이 아닌 사람의 신원"을 담고 있어서,
 * 위조나 타입 바꿔치기가 통하면 남의 소셜 계정으로 가입할 수 있다.
 */
class JwtProviderSignupTokenTest {

    private static final String SECRET = "test-secret-key-for-jwt-provider-32bytes!";

    /** signup 토큰 수명만 바꿔가며 provider를 만든다 (만료 케이스 검증용) */
    private JwtProvider provider(long signupValiditySeconds) {
        return new JwtProvider(SECRET, 3600, 1209600, signupValiditySeconds);
    }

    private final SignupClaims claims = new SignupClaims(
            "KAKAO", "kakao-1", "user@example.com", "홍길동", "https://toget.com/images/kakao.png");

    @Test
    @DisplayName("발급한 가입 토큰에서 소셜 식별자와 프로필 정보를 그대로 복원한다")
    void createAndParse_roundTrip() {
        JwtProvider jwtProvider = provider(600);

        SignupClaims parsed = jwtProvider.parseSignupToken(jwtProvider.createSignupToken(claims));

        assertThat(parsed).isEqualTo(claims);
    }

    @Test
    @DisplayName("소셜에서 못 받아온 값(동의 안 함)은 null 그대로 복원된다 (\"null\" 문자열이 되면 안 된다)")
    void createAndParse_withMissingOptionalClaims() {
        JwtProvider jwtProvider = provider(600);
        SignupClaims minimal = new SignupClaims("GOOGLE", "google-sub-1", null, null, null);

        SignupClaims parsed = jwtProvider.parseSignupToken(jwtProvider.createSignupToken(minimal));

        assertThat(parsed.provider()).isEqualTo("GOOGLE");
        assertThat(parsed.oAuthId()).isEqualTo("google-sub-1");
        assertThat(parsed.email()).isNull();
        assertThat(parsed.name()).isNull();
        assertThat(parsed.profileImageUrl()).isNull();
    }

    @Test
    @DisplayName("만료된 가입 토큰은 401로 거절한다")
    void expiredSignupToken_isRejected() {
        // 수명을 음수로 주면 발급 즉시 만료 상태가 된다
        JwtProvider jwtProvider = provider(-1);
        String token = jwtProvider.createSignupToken(claims);

        assertThatThrownBy(() -> jwtProvider.parseSignupToken(token))
                .isInstanceOf(UserException.class)
                .extracting(e -> ((UserException) e).getCode())
                .isEqualTo(UserErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("access token을 가입 토큰 자리에 꽂으면 401 — 토큰 타입 바꿔치기 차단")
    void accessToken_cannotBeUsedAsSignupToken() {
        JwtProvider jwtProvider = provider(600);
        String accessToken = jwtProvider.createAccessToken(1L);

        assertThatThrownBy(() -> jwtProvider.parseSignupToken(accessToken))
                .isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("가입 토큰을 access token 자리에 꽂으면 401 — 회원이 아닌데 API를 호출할 수 없다")
    void signupToken_cannotBeUsedAsAccessToken() {
        JwtProvider jwtProvider = provider(600);
        String signupToken = jwtProvider.createSignupToken(claims);

        assertThatThrownBy(() -> jwtProvider.parse(signupToken, JwtProvider.TOKEN_TYPE_ACCESS))
                .isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("서명이 변조된 가입 토큰은 401로 거절한다")
    void tamperedSignature_isRejected() {
        JwtProvider jwtProvider = provider(600);
        String token = jwtProvider.createSignupToken(claims);
        // 서명의 마지막 글자만 바꿔치기
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");

        assertThatThrownBy(() -> jwtProvider.parseSignupToken(tampered))
                .isInstanceOf(UserException.class);
    }

    @Test
    @DisplayName("다른 비밀 키로 만든 가입 토큰은 401로 거절한다")
    void tokenFromDifferentSecret_isRejected() {
        String forged = new JwtProvider("another-secret-key-that-is-32-bytes-long!", 3600, 1209600, 600)
                .createSignupToken(new SignupClaims("KAKAO", "victim-oauth-id", null, null, null));

        assertThatThrownBy(() -> provider(600).parseSignupToken(forged))
                .isInstanceOf(UserException.class);
    }
}
