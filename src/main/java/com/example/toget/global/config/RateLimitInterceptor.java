package com.example.toget.global.config;

import com.example.toget.global.annotation.RateLimit;
import com.example.toget.global.apiPayload.code.GeneralErrorCode;
import com.example.toget.global.apiPayload.exception.ProjectException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link RateLimit}이 붙은 핸들러에 대해 클라이언트 IP 단위로 요청 빈도를 제한하는 인터셉터.
 *
 * <p>[저장소] 단일 인스턴스 배포 기준으로 인메모리 ConcurrentHashMap에 IP+메서드 단위
 * 버킷을 보관한다. 여러 인스턴스로 스케일아웃하면 인스턴스마다 별도 버킷을 갖게 되어
 * 실제 한도가 인스턴스 수만큼 느슨해지므로, 그 시점에는 Redis 등 공유 저장소로 교체해야 한다.
 *
 * <p>[IP 판별] 이 서비스는 Nginx 뒤에서 동작한다(application.yaml의
 * forward-headers-strategy: framework 참고). Nginx는 보통
 * {@code proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;}로 클라이언트가
 * 보낸 기존 헤더 뒤에 실제 접속 IP를 이어 붙이므로, 신뢰할 수 있는 값은 맨 앞이 아니라
 * 맨 뒤(마지막) 항목이다. 맨 앞 값을 쓰면 클라이언트가 X-Forwarded-For 헤더에 임의의
 * 문자열을 직접 실어 보내는 것만으로 매 요청마다 다른 버킷 키를 만들어 rate limit을
 * 통째로 우회할 수 있다. 헤더가 없으면 request.getRemoteAddr()로 폴백한다.
 *
 * <p>[메모리 누수] 버킷 Map은 만료·삭제 로직이 없어 서로 다른 IP가 계속 늘어나면
 * 무한정 커진다. 트래픽 규모가 커지면 Caffeine 등 TTL 캐시로 교체를 고려해야 한다.
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        String key = resolveClientIp(request) + ":" + handlerMethod.getMethod();
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket(rateLimit));

        if (!bucket.tryConsume(1)) {
            log.warn("요청 빈도 제한을 초과했습니다. key={}, uri={}", key, request.getRequestURI());
            throw new ProjectException(GeneralErrorCode.TOO_MANY_REQUESTS);
        }
        return true;
    }

    private Bucket newBucket(RateLimit rateLimit) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(rateLimit.capacity())
                .refillGreedy(rateLimit.capacity(), Duration.ofSeconds(rateLimit.refillSeconds()))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // Nginx가 실제 접속 IP를 맨 뒤에 이어 붙이므로, 신뢰 가능한 값은 마지막 항목이다.
            // 맨 앞 값은 클라이언트가 임의로 조작해 보낼 수 있어 그대로 쓰면 rate limit이 우회된다.
            String[] hops = forwardedFor.split(",");
            String lastHop = hops[hops.length - 1].trim();
            if (!lastHop.isEmpty()) {
                return lastHop;
            }
        }
        return request.getRemoteAddr();
    }
}
