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
 * <p>[IP 판별] 프록시/로드밸런서 뒤에 있는 배포를 고려해 X-Forwarded-For 헤더를 우선
 * 사용하고, 없으면 request.getRemoteAddr()로 폴백한다. X-Forwarded-For는 클라이언트가
 * 임의로 조작해 우회할 수 있는 값이라 강한 보장은 아니지만, 스크립트 도배처럼 프록시를
 * 거치지 않는 단순 반복 요청을 막는 데는 충분하다.
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
            // 여러 프록시를 거치면 콤마로 누적되므로 최초 클라이언트 IP(맨 앞)만 사용
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
