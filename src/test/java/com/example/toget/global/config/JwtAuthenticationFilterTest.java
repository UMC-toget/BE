package com.example.toget.global.config;

import com.example.toget.domain.user.exception.UserErrorCode;
import com.example.toget.domain.user.exception.UserException;
import com.example.toget.domain.user.service.JwtClaims;
import com.example.toget.domain.user.service.JwtProvider;
import com.example.toget.global.apiPayload.code.GeneralErrorCode;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * JwtAuthenticationFilter 단위 테스트 (issue #55).
 *
 * 핵심 계약: "토큰을 들고 온 요청은 반드시 회원으로 판정되거나 401로 표시."
 * 즉 유효하지 않은 토큰이 비회원(인증 정보 없음)으로 통과되어서는 안 된다.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final Long USER_ID = 42L;
    private static final String VALID_TOKEN = "valid.access.token";

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private FilterChain filterChain;

    // 응답 JSON 작성은 부수적인 관심사이므로 실제 구현을 그대로 쓴다 (포맷까지 함께 검증)
    private final SecurityErrorResponseWriter errorResponseWriter = new SecurityErrorResponseWriter();

    /** SecurityContextHolder는 ThreadLocal이라 테스트 간 값이 새지 않도록 매번 비운다 */
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private JwtAuthenticationFilter filter() {
        return new JwtAuthenticationFilter(jwtProvider, errorResponseWriter);
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 인증 없이 통과시킨다 — 비회원 요청")
    void passesThroughWithoutAuthorizationHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/fundings/1/contributions");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response); // 컨트롤러까지 도달해야 함
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute("loginUserId")).isNull();
    }

    @Test
    @DisplayName("유효한 토큰이면 principal에 userId를 담고 통과시킨다")
    void authenticatesWithValidToken() throws Exception {
        given(jwtProvider.parse(eq(VALID_TOKEN), eq(JwtProvider.TOKEN_TYPE_ACCESS)))
                .willReturn(new JwtClaims(USER_ID, null, JwtProvider.TOKEN_TYPE_ACCESS));
        given(jwtProvider.getUserId(any(JwtClaims.class))).willReturn(USER_ID);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/fundings/1/contributions");
        request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(USER_ID);
        assertThat(request.getAttribute("loginUserId")).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("만료·위조 토큰이면 401로 끊고 다음 필터로 넘기지 않는다 — 비회원으로 집계되지 않아야 함")
    void rejectsInvalidToken() throws Exception {
        given(jwtProvider.parse(any(), any())).willThrow(new UserException(UserErrorCode.UNAUTHORIZED));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/fundings/1/contributions");
        request.addHeader("Authorization", "Bearer expired.or.forged");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any()); // 체인이 끊겨야 함 — 이 검증이 이슈의 핵심
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Bearer 형식이 아닌 Authorization 헤더도 401로 거부한다")
    void rejectsNonBearerAuthorizationHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/fundings/1/contributions");
        request.addHeader("Authorization", "Basic dXNlcjpwYXNzd29yZA==");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("401 응답 본문이 ApiResponse 포맷을 따른다")
    void writesApiResponseFormatOn401() throws Exception {
        given(jwtProvider.parse(any(), any())).willThrow(new UserException(UserErrorCode.UNAUTHORIZED));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/fundings/1/contributions");
        request.addHeader("Authorization", "Bearer expired.token.here");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, filterChain);

        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString())
                .contains("\"isSuccess\":false")
                .contains(GeneralErrorCode.UNAUTHORIZED.getCode());
    }

    @Test
    @DisplayName("토큰 재발급 요청은 만료된 access token이 붙어 있어도 통과시킨다 — 로그인 루프 방지")
    void toleratesInvalidTokenOnRefresh() throws Exception {
        given(jwtProvider.parse(any(), any())).willThrow(new UserException(UserErrorCode.UNAUTHORIZED));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/tokens/refresh");
        request.addHeader("Authorization", "Bearer expired.token.here");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("로그아웃은 인증이 필요하므로 만료 토큰을 401로 거부한다")
    void rejectsInvalidTokenOnLogout() throws Exception {
        given(jwtProvider.parse(any(), any())).willThrow(new UserException(UserErrorCode.UNAUTHORIZED));

        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/auth/tokens/me");
        request.addHeader("Authorization", "Bearer expired.token.here");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("소문자 bearer 스킴도 인증 스킴으로 받아들인다 — RFC 7235상 대소문자 무관")
    void acceptsLowercaseBearerScheme() throws Exception {
        given(jwtProvider.parse(eq(VALID_TOKEN), eq(JwtProvider.TOKEN_TYPE_ACCESS)))
                .willReturn(new JwtClaims(USER_ID, null, JwtProvider.TOKEN_TYPE_ACCESS));
        given(jwtProvider.getUserId(any(JwtClaims.class))).willReturn(USER_ID);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/fundings/1/contributions");
        request.addHeader("Authorization", "bearer " + VALID_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("경로 끝 슬래시가 붙어도 로그아웃 경로로 인식해 401로 거부한다")
    void normalizesTrailingSlashOnLogoutPath() throws Exception {
        given(jwtProvider.parse(any(), any())).willThrow(new UserException(UserErrorCode.UNAUTHORIZED));

        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/auth/tokens/me/");
        request.addHeader("Authorization", "Bearer expired.token.here");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("contextPath가 있어도 경로 판정이 어긋나지 않는다 — Nginx X-Forwarded-Prefix 대비")
    void matchesPathIgnoringContextPath() throws Exception {
        given(jwtProvider.parse(any(), any())).willThrow(new UserException(UserErrorCode.UNAUTHORIZED));

        // getRequestURI()는 contextPath를 포함하지만 경로 판정은 그 아래 경로로 이뤄져야 한다
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/gateway/api/v1/auth/tokens/refresh");
        request.setContextPath("/gateway");
        request.addHeader("Authorization", "Bearer expired.token.here");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response); // 재발급 경로로 인식되어 통과해야 함
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Swagger 경로는 만료 토큰이 붙어 있어도 필터를 타지 않는다")
    void skipsSwaggerPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        request.addHeader("Authorization", "Bearer expired.token.here");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter().doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
