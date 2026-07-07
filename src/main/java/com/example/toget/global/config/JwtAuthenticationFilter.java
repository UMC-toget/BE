package com.example.toget.global.config;

import com.example.toget.domain.user.exception.UserException;
import com.example.toget.domain.user.service.JwtClaims;
import com.example.toget.domain.user.service.JwtProvider;
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
 *  4. 검증 실패/토큰 없음 → 인증 정보 없이 통과시키고,
 *     보호된 경로라면 SecurityConfig의 authenticationEntryPoint가 401로 응답한다.
 *     (필터가 직접 401을 만들지 않는 이유: permitAll 경로는 토큰이 없어도 접근돼야 하기 때문)
 *
 * OncePerRequestFilter: 같은 요청에 대해 필터가 딱 한 번만 실행되도록 보장해 주는 편의 부모 클래스.
 */
@Component
@RequiredArgsConstructor // final 필드를 받는 생성자를 롬복이 자동 생성 → 스프링이 생성자 주입으로 JwtProvider를 넣어줌
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String LOGIN_USER_ID_ATTRIBUTE = "loginUserId";
    private static final String BEARER_PREFIX = "Bearer "; // "Bearer <토큰>" 형식이 HTTP 표준 관례

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        // 헤더가 있고 Bearer 형식일 때만 인증 시도 — 아니면 "익명 사용자"로 그냥 통과
        if (StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX)) {
            authenticate(request, authorization.substring(BEARER_PREFIX.length()));
        }
        filterChain.doFilter(request, response); // 다음 필터(최종적으로는 컨트롤러)로 요청을 넘김
    }

    private void authenticate(HttpServletRequest request, String token) {
        try {
            // access 타입인지까지 검증 — refresh token으로는 API를 호출할 수 없다
            JwtClaims claims = jwtProvider.parse(token, JwtProvider.TOKEN_TYPE_ACCESS);
            Long userId = jwtProvider.getUserId(claims);
            // SecurityContext에 넣을 인증 객체. principal(주체) 자리에 userId를 담는다.
            // 파라미터: (principal, credentials=비밀번호류는 없으므로 null, authorities=권한 목록)
            var authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());
            // SecurityContextHolder: 현재 요청(스레드)에 인증 정보를 보관하는 저장소.
            // 여기 값이 있어야 SecurityConfig의 .authenticated() 검사를 통과한다.
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute(LOGIN_USER_ID_ATTRIBUTE, userId);
        } catch (UserException ignored) {
            // 만료·위조 토큰은 인증 정보를 비운 채 통과 → 보호된 경로면 뒤에서 401 처리됨
            SecurityContextHolder.clearContext();
        }
    }
}
