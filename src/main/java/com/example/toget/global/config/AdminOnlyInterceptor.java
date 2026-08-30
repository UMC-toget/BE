package com.example.toget.global.config;

import com.example.toget.domain.user.entity.User;
import com.example.toget.domain.user.exception.UserErrorCode;
import com.example.toget.domain.user.exception.UserException;
import com.example.toget.domain.user.service.ActiveUserReader;
import com.example.toget.global.annotation.AdminOnly;
import com.example.toget.global.apiPayload.code.GeneralErrorCode;
import com.example.toget.global.apiPayload.exception.ProjectException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * {@link AdminOnly}가 붙은 핸들러에 대해 요청자가 설정된 관리자 계정인지 검사하는 인터셉터.
 *
 * <p>[요청 흐름에서의 위치]
 * JwtAuthenticationFilter(인증: 누구인가) → SecurityConfig(인가: 로그인했는가)
 * → <b>이 인터셉터</b>(인가: 관리자인가) → 컨트롤러
 *
 * <p>시큐리티 단계에서 이미 로그인 여부는 걸러지므로, 여기서는 "그 로그인 사용자가
 * 관리자인가"만 판정한다. 불일치 시 ProjectException을 던지고,
 * preHandle에서 발생한 예외는 DispatcherServlet이 HandlerExceptionResolver로 넘기므로
 * GeneralExceptionAdvice가 공통 ApiResponse 포맷(COMMON403_1)으로 응답한다.
 *
 * <p>WebConfig의 addInterceptors에서 MVC에 등록된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminOnlyInterceptor implements HandlerInterceptor {

    private final AdminProperties adminProperties;
    private final ActiveUserReader activeUserReader;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 정적 리소스·에러 디스패치 등 컨트롤러 메서드가 아닌 핸들러는 검사 대상이 아니다
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if (!requiresAdmin(handlerMethod)) {
            return true;
        }

        // fail-closed — 관리자 식별값이 비어 있으면 통과가 아니라 차단한다.
        // (설정 누락이 관리자 API 전면 개방으로 이어지는 것이 최악의 시나리오)
        if (!adminProperties.isConfigured()) {
            log.error("admin.email이 설정되지 않아 관리자 API 요청을 차단했습니다. uri={}", request.getRequestURI());
            throw new ProjectException(GeneralErrorCode.FORBIDDEN);
        }

        // 탈퇴 회원은 여기서 401로 걸러진다 (토큰은 유효해도 계정이 살아있는지 DB로 재확인)
        User user = activeUserReader.getActiveUser(resolveLoginUserId(request));

        boolean isAdmin = adminProperties.accounts().stream()
                .anyMatch(admin -> user.isAdmin(admin.provider(), admin.email()));
        if (!isAdmin) {
            log.warn("관리자 전용 API에 비관리자 접근이 차단되었습니다. userId={}, uri={}",
                    user.getId(), request.getRequestURI());
            throw new ProjectException(GeneralErrorCode.FORBIDDEN);
        }
        return true;
    }

    /** 메서드 단위와 클래스 단위 모두 지원 — 컨트롤러 전체를 관리자 전용으로 묶을 수도 있다 */
    private boolean requiresAdmin(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(AdminOnly.class)
                || handlerMethod.getBeanType().isAnnotationPresent(AdminOnly.class);
    }

    /**
     * 로그인 사용자 ID 추출 — LoginUserIdArgumentResolver와 동일한 순서로 조회한다.
     * 둘 다 없다면 인증되지 않은 요청이므로 401. (보호 경로는 시큐리티가 먼저 막으므로 사실상 안전망)
     */
    private Long resolveLoginUserId(HttpServletRequest request) {
        Object attribute = request.getAttribute(JwtAuthenticationFilter.LOGIN_USER_ID_ATTRIBUTE);
        if (attribute instanceof Long userId) {
            return userId;
        }
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new UserException(UserErrorCode.UNAUTHORIZED);
    }
}
