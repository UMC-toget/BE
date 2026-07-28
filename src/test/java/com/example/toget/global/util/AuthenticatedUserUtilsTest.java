package com.example.toget.global.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AuthenticatedUserUtils 단위 테스트 (issue #55).
 * principal에 들어올 수 있는 값들을 실제 스프링 시큐리티 인증 객체로 재현해 판정 결과를 검증한다.
 */
class AuthenticatedUserUtilsTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증 객체가 없으면 null을 반환한다")
    void returnsNullWhenNoAuthentication() {
        assertThat(AuthenticatedUserUtils.getCurrentUserIdOrNull()).isNull();
        assertThat(AuthenticatedUserUtils.isAuthenticated()).isFalse();
    }

    @Test
    @DisplayName("익명 사용자(anonymousUser)면 null을 반환한다")
    void returnsNullForAnonymousUser() {
        // 스프링 시큐리티가 permitAll 경로에서 실제로 세팅하는 형태를 그대로 재현
        var anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        assertThat(AuthenticatedUserUtils.getCurrentUserIdOrNull()).isNull();
        assertThat(AuthenticatedUserUtils.isAuthenticated()).isFalse();
    }

    @Test
    @DisplayName("익명 principal이 기본값이 아니어도 null을 반환한다 — 문자열이 아닌 타입으로 판정")
    void returnsNullForAnonymousTokenWithCustomPrincipal() {
        // principal이 "anonymousUser"가 아니어도 익명 인증이라는 사실은 타입으로 드러난다
        var anonymous = new AnonymousAuthenticationToken(
                "key", "guest", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        assertThat(AuthenticatedUserUtils.getCurrentUserIdOrNull()).isNull();
    }

    @Test
    @DisplayName("principal이 Long이 아닌 타입이면 null을 반환한다")
    void returnsNullWhenPrincipalTypeIsNotLong() {
        // 다른 인증 방식이 문자열 principal을 넣더라도 userId로 오인해선 안 된다
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("someone@example.com", null, List.of()));

        assertThat(AuthenticatedUserUtils.getCurrentUserIdOrNull()).isNull();
    }

    @Test
    @DisplayName("principal이 Long이면 그 값을 userId로 반환한다")
    void returnsUserIdWhenPrincipalIsLong() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(42L, null, List.of()));

        assertThat(AuthenticatedUserUtils.getCurrentUserIdOrNull()).isEqualTo(42L);
        assertThat(AuthenticatedUserUtils.isAuthenticated()).isTrue();
    }
}
