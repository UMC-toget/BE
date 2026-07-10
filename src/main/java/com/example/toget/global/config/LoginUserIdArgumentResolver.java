package com.example.toget.global.config;

import com.example.toget.domain.user.controller.LoginUserId;
import com.example.toget.domain.user.exception.UserErrorCode;
import com.example.toget.domain.user.exception.UserException;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 컨트롤러 파라미터의 @LoginUserId Long userId 에 로그인한 사용자 ID를 넣어주는 리졸버.
 *
 * [동작 원리] 스프링 MVC는 컨트롤러 메서드를 호출하기 전에, 각 파라미터를 누가 채울지
 * 등록된 HandlerMethodArgumentResolver들에게 물어본다(supportsParameter).
 * true를 반환한 리졸버의 resolveArgument 결과가 그 파라미터 값으로 들어간다.
 * (@RequestBody, @PathVariable도 사실 같은 방식으로 동작하는 내장 리졸버들이다)
 *
 * 이 리졸버는 WebConfig의 addArgumentResolvers에서 MVC에 등록된다.
 * 덕분에 컨트롤러마다 토큰을 직접 파싱하는 코드를 반복하지 않아도 된다.
 */
@Component
public class LoginUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    /** 이 리졸버가 담당할 파라미터인지 판별: @LoginUserId가 붙어 있고 타입이 Long인 경우만 */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUserId.class)
                && Long.class.isAssignableFrom(parameter.getParameterType());
    }

    /** 실제 값 결정: JwtAuthenticationFilter가 심어 둔 사용자 ID를 꺼낸다 */
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        // 1순위: 필터가 request attribute로 넣어 둔 값
        Object attribute = webRequest.getAttribute(JwtAuthenticationFilter.LOGIN_USER_ID_ATTRIBUTE,
                NativeWebRequest.SCOPE_REQUEST);
        if (attribute instanceof Long userId) { // instanceof 패턴 매칭: 타입 확인과 캐스팅을 한 번에
            return userId;
        }

        // 2순위: SecurityContext의 principal (필터가 인증 성공 시 함께 저장해 둠)
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        // 둘 다 없다 = 인증 안 된 요청이 여기까지 온 것 → 401
        // (보호된 경로는 시큐리티가 먼저 막아주므로 사실상 안전망)
        throw new UserException(UserErrorCode.UNAUTHORIZED);
    }
}
