package com.example.toget.global.config;

import com.example.toget.domain.user.exception.UserException;
import com.example.toget.domain.user.service.JwtClaims;
import com.example.toget.domain.user.service.JwtProvider;
import com.example.toget.global.apiPayload.code.GeneralErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 모든 HTTP 요청에서 JWT를 꺼내 "이 요청이 누구인지"를 확정하는 필터.
 *
 * [요청 한 건의 인증 흐름]
 *  1. 클라이언트가 "Authorization: Bearer <access token>" 헤더를 붙여 요청
 *  2. 이 필터가 컨트롤러보다 먼저 실행되어 토큰을 검증 (SecurityConfig의 addFilterBefore로 등록됨)
 *  3. 검증 성공 → SecurityContext에 인증 정보 저장
 *     → 이후 SecurityConfig의 authenticated() 검사를 통과하고,
 *       컨트롤러의 @LoginUserId 파라미터로 userId가 주입된다 (LoginUserIdArgumentResolver)
 *  4. 헤더 없음 → 인증 정보 없이 통과. 비회원 허용(permitAll) 경로면 그대로 진행되고,
 *     보호된 경로라면 SecurityConfig의 authenticationEntryPoint가 401로 응답한다.
 *  5. 헤더는 있는데 토큰이 만료·위조·형식 오류 → 이 필터가 즉시 401로 응답하고 체인을 끊는다.
 *     (예외: 토큰 발급/재발급 경로는 만료 토큰을 무시하고 통과 — 아래 AUTH_TOKEN_PATH_PREFIX 주석 참고)
 *
 * [왜 잘못된 토큰을 그냥 통과시키지 않는가 — issue #55]
 * 비회원 허용 엔드포인트가 생기면서, 유효하지 않은 토큰을 통과시키면 그 요청이 permitAll 경로에서
 * "인증 정보 없는 요청 = 비회원"으로 조용히 집계된다. 만료된 토큰을 든 회원의 후원이 비회원 후원으로
 * 저장되는 식으로 데이터 신뢰도가 깨지므로, 토큰을 들고 온 요청은 반드시
 * "회원으로 판정" 또는 "401" 둘 중 하나로만 끝나도록 보장한다.
 *
 * OncePerRequestFilter: 같은 요청에 대해 필터가 딱 한 번만 실행되도록 보장해 주는 편의 부모 클래스.
 */
@Component
@RequiredArgsConstructor // final 필드를 받는 생성자를 롬복이 자동 생성 → 스프링이 생성자 주입으로 의존성을 넣어줌
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String LOGIN_USER_ID_ATTRIBUTE = "loginUserId";

    // "Bearer <토큰>" 형식이 HTTP 표준 관례.
    // RFC 7235상 인증 스킴 이름은 대소문자를 구분하지 않으므로 "bearer", "BEARER"도 받아들인다.
    private static final String BEARER_PREFIX = "Bearer ";

    // 토큰 검증을 아예 태우지 않을 경로.
    // Swagger는 API 문서를 보여줄 뿐이라 인증과 무관한데, 브라우저에 남아 있던 만료 토큰 때문에
    // 문서 화면 자체가 401로 막히면 개발 흐름이 끊긴다.
    // ※ SecurityConfig의 allowAllUris에도 같은 Swagger 경로가 있다. 둘은 목적이 다르다
    //   (저기는 "인가 없이 접근 허용", 여기는 "토큰 검증 자체를 생략"). Swagger 경로를 바꿀 일이
    //   생기면 두 곳을 함께 고쳐야 한다.
    private static final List<String> SKIP_PATH_PREFIXES = List.of(
            "/swagger-ui",
            "/swagger-resources",
            "/v3/api-docs"
    );

    // 잘못된 토큰이 와도 401로 끊지 않고 "무시하고 통과"시킬 경로.
    // 소셜 로그인·토큰 재발급은 클라이언트가 만료된 access token을 그대로 헤더에 달고 호출하는 것이
    // 오히려 정상 시나리오다(대부분의 HTTP 인터셉터가 헤더를 자동으로 붙인다).
    // 여기서까지 401을 내면 만료된 사용자가 재발급으로 복구할 길이 막혀 로그인 루프에 빠진다.
    // 이 경로들은 body의 refresh token / 인가 코드로만 인증하므로 principal이 필요 없다.
    // ※ SecurityConfig의 requestMatchers("/api/v1/auth/tokens/**", DELETE ".../me")와 같은 사실을
    //   두 곳에서 표현하고 있다. /auth/tokens 아래에 엔드포인트를 추가하면 두 파일을 함께 봐야 한다.
    private static final String AUTH_TOKEN_PATH_PREFIX = "/api/v1/auth/tokens/";
    private static final String LOGOUT_PATH = "/api/v1/auth/tokens/me"; // 로그아웃만은 인증 필요

    private final JwtProvider jwtProvider;
    private final SecurityErrorResponseWriter errorResponseWriter;

    /** true를 반환하면 이 요청에는 필터가 실행되지 않는다 (OncePerRequestFilter가 제공하는 훅) */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = pathWithinApplication(request);
        return SKIP_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    /**
     * 경로 비교용 정규화.
     *
     * getRequestURI()를 그대로 쓰지 않는 이유가 두 가지다.
     *  1) getRequestURI()에는 contextPath가 포함되지만 SecurityConfig의 requestMatchers는 이를 제외하고
     *     매칭한다. application.yaml에 forward-headers-strategy가 켜져 있어 Nginx가 X-Forwarded-Prefix를
     *     보내면 contextPath가 생기는데, 그러면 이 필터의 경로 판정만 조용히 어긋난다.
     *  2) 뒤에 붙은 "/"가 있으면 equals 비교가 빗나간다. ("/auth/tokens/me/"가 로그아웃으로 안 잡힘)
     */
    private String pathWithinApplication(HttpServletRequest request) {
        String path = request.getRequestURI();

        String contextPath = request.getContextPath();
        if (StringUtils.hasLength(contextPath) && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path.isEmpty() ? "/" : path;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        // 헤더가 아예 없으면 "비회원 요청"으로 보고 그대로 통과시킨다.
        // 보호된 경로라면 뒤쪽 시큐리티 단계에서 401이 만들어진다.
        if (!StringUtils.hasText(authorization)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 헤더를 붙였다는 건 "회원으로 인증해 달라"는 의사표시다.
        // 여기서부터는 인증에 성공하거나 401로 끝나야 하며, 비회원으로 흘러가서는 안 된다.
        // 스킴 이름은 대소문자 무관 비교 (RFC 7235). Bearer가 아닌 인증 방식은 지원하지 않는다.
        if (!authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            handleInvalidToken(request, response, filterChain);
            return;
        }

        try {
            authenticate(request, authorization.substring(BEARER_PREFIX.length()));
        } catch (UserException e) { // 만료·서명 불일치·형식 오류
            handleInvalidToken(request, response, filterChain);
            return;
        }

        filterChain.doFilter(request, response); // 다음 필터(최종적으로는 컨트롤러)로 요청을 넘김
    }

    /**
     * 유효하지 않은 토큰을 만났을 때의 분기.
     * 토큰 발급 계열 경로는 무시하고 통과, 나머지는 401로 끊는다.
     */
    private void handleInvalidToken(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        SecurityContextHolder.clearContext();
        if (toleratesInvalidToken(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        rejectInvalidToken(response);
    }

    /** 잘못된 토큰을 401이 아니라 "인증 정보 없음"으로 취급해도 되는 경로인지 */
    private boolean toleratesInvalidToken(HttpServletRequest request) {
        String path = pathWithinApplication(request);
        return path.startsWith(AUTH_TOKEN_PATH_PREFIX) && !path.equals(LOGOUT_PATH);
    }

    private void authenticate(HttpServletRequest request, String token) {
        // access 타입인지까지 검증 — refresh token으로는 API를 호출할 수 없다.
        // 검증 실패 시 JwtProvider가 UserException(UNAUTHORIZED)을 던지고, 호출부에서 401로 변환한다.
        JwtClaims claims = jwtProvider.parse(token, JwtProvider.TOKEN_TYPE_ACCESS);
        Long userId = jwtProvider.getUserId(claims);
        // SecurityContext에 넣을 인증 객체. principal(주체) 자리에 userId를 담는다.
        // 파라미터: (principal, credentials=비밀번호류는 없으므로 null, authorities=권한 목록)
        var authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        // SecurityContextHolder: 현재 요청(스레드)에 인증 정보를 보관하는 저장소.
        // 여기 값이 있어야 SecurityConfig의 .authenticated() 검사를 통과한다.
        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.setAttribute(LOGIN_USER_ID_ATTRIBUTE, userId);
    }

    /**
     * 유효하지 않은 토큰을 들고 온 요청을 401로 끊는다.
     * 체인을 이어가지 않으므로 permitAll 경로라도 컨트롤러까지 도달하지 않는다 — 의도된 동작이다.
     * (실패 사유를 세분화해 알려주면 공격자에게 힌트가 되므로 일괄 UNAUTHORIZED로 응답한다)
     */
    private void rejectInvalidToken(HttpServletResponse response) throws IOException {
        errorResponseWriter.write(response, GeneralErrorCode.UNAUTHORIZED);
    }
}
