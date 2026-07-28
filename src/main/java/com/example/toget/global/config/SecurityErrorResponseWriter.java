package com.example.toget.global.config;

import com.example.toget.global.apiPayload.ApiResponse;
import com.example.toget.global.apiPayload.code.BaseErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 시큐리티 필터 단계에서 발생한 오류를 ApiResponse JSON으로 내려주는 작성기.
 * 필터 체인은 DispatcherServlet보다 앞에서 실행되므로 @RestControllerAdvice(GeneralExceptionAdvice)가
 * 닿지 않는다. 그래서 SecurityConfig의 authenticationEntryPoint / accessDeniedHandler와
 * JwtAuthenticationFilter가 각자 응답 JSON을 직접 만들어야 하는데,
 * 그 로직이 흩어지면 같은 401인데도 응답 포맷이 갈라질 수 있다.
 * 작성 책임을 이 클래스 하나로 모아 "시큐리티 단계의 실패 응답은 항상 같은 모양"을 보장한다.
 */
@Component
public class SecurityErrorResponseWriter {

    // 스프링 MVC의 ObjectMapper 빈은 필터 시점에 주입받아 쓸 수는 있으나,
    // 이 클래스는 ApiResponse 직렬화만 하면 되므로 설정에 의존하지 않는 전용 인스턴스를 쓴다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 주어진 에러코드로 상태·본문을 채운 실패 응답을 기록한다.
     * 이미 커밋된 응답(다른 곳에서 먼저 써버린 경우)에는 아무것도 하지 않아 IllegalStateException을 피한다.
     */
    public void write(HttpServletResponse response, BaseErrorCode errorCode) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.onFailure(errorCode, null)));
    }
}
