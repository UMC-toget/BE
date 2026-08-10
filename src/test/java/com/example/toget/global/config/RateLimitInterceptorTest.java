package com.example.toget.global.config;

import com.example.toget.global.annotation.RateLimit;
import com.example.toget.global.apiPayload.code.GeneralErrorCode;
import com.example.toget.global.apiPayload.exception.ProjectException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RateLimitInterceptor 단위 테스트.
 *
 * [설계 포인트]
 *  - MockMvc 전체를 띄우지 않고, HandlerInterceptor.preHandle을 직접 호출해
 *    "같은 IP로 capacity 초과 요청을 보내면 429가 던져지는지"만 좁게 검증한다.
 *  - @RateLimit이 없는 핸들러는 그냥 통과해야 한다 — 이 인터셉터가 전체 API에
 *    등록되므로(WebConfig), 무관한 API까지 막아버리는 회귀를 방지한다.
 */
class RateLimitInterceptorTest {

    private final RateLimitInterceptor interceptor = new RateLimitInterceptor();

    @Test
    @DisplayName("@RateLimit이 없는 핸들러는 항상 통과한다")
    void handler_without_rate_limit_always_passes() throws NoSuchMethodException {
        HandlerMethod handlerMethod = handlerMethod("noLimit");
        MockHttpServletRequest request = new MockHttpServletRequest();

        for (int i = 0; i < 10; i++) {
            assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), handlerMethod)).isTrue();
        }
    }

    @Test
    @DisplayName("capacity만큼은 통과하고, 초과하면 429(ProjectException)를 던진다")
    void exceeding_capacity_throws_too_many_requests() throws NoSuchMethodException {
        HandlerMethod handlerMethod = handlerMethod("limited");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("1.2.3.4");

        // capacity = 2 → 처음 2번은 통과
        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), handlerMethod)).isTrue();
        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), handlerMethod)).isTrue();

        // 3번째는 한도 초과로 차단
        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), handlerMethod))
                .isInstanceOf(ProjectException.class)
                .satisfies(ex -> assertThat(((ProjectException) ex).getCode())
                        .isEqualTo(GeneralErrorCode.TOO_MANY_REQUESTS));
    }

    @Test
    @DisplayName("IP가 다르면 서로의 한도에 영향을 주지 않는다")
    void different_ips_have_independent_buckets() throws NoSuchMethodException {
        HandlerMethod handlerMethod = handlerMethod("limited");

        MockHttpServletRequest requestA = new MockHttpServletRequest();
        requestA.setRemoteAddr("1.1.1.1");
        MockHttpServletRequest requestB = new MockHttpServletRequest();
        requestB.setRemoteAddr("2.2.2.2");

        // A가 한도(2)를 다 써도
        interceptor.preHandle(requestA, new MockHttpServletResponse(), handlerMethod);
        interceptor.preHandle(requestA, new MockHttpServletResponse(), handlerMethod);

        // B는 영향받지 않고 여전히 통과한다
        assertThat(interceptor.preHandle(requestB, new MockHttpServletResponse(), handlerMethod)).isTrue();
    }

    @Test
    @DisplayName("X-Forwarded-For 헤더가 있으면 remoteAddr 대신 그 값을 IP로 사용한다")
    void uses_x_forwarded_for_when_present() throws NoSuchMethodException {
        HandlerMethod handlerMethod = handlerMethod("limited");

        MockHttpServletRequest requestA = new MockHttpServletRequest();
        requestA.setRemoteAddr("10.0.0.1"); // 프록시 IP는 동일
        requestA.addHeader("X-Forwarded-For", "9.9.9.9, 10.0.0.1");

        MockHttpServletRequest requestB = new MockHttpServletRequest();
        requestB.setRemoteAddr("10.0.0.1"); // 프록시 IP는 동일
        requestB.addHeader("X-Forwarded-For", "8.8.8.8, 10.0.0.1");

        // 서로 다른 실제 클라이언트(X-Forwarded-For)이므로 A가 한도를 다 써도
        interceptor.preHandle(requestA, new MockHttpServletResponse(), handlerMethod);
        interceptor.preHandle(requestA, new MockHttpServletResponse(), handlerMethod);

        // B는 별도 버킷이라 여전히 통과한다
        assertThat(interceptor.preHandle(requestB, new MockHttpServletResponse(), handlerMethod)).isTrue();
    }

    private HandlerMethod handlerMethod(String methodName) throws NoSuchMethodException {
        Method method = TestController.class.getMethod(methodName);
        return new HandlerMethod(new TestController(), method);
    }

    static class TestController {
        public void noLimit() {
        }

        @RateLimit(capacity = 2, refillSeconds = 60)
        public void limited() {
        }
    }
}
