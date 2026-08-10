package com.example.toget.global.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 요청 빈도 제한이 필요한 API임을 표시하는 어노테이션.
 * 컨트롤러 메서드에 붙이면 RateLimitInterceptor가 클라이언트 IP + 메서드 시그니처를 키로
 * Bucket4j 토큰 버킷을 조회해, 한도를 초과한 요청은 429(COMMON429_1)로 차단한다.
 *
 * <p>어노테이션 자체는 "표시"일 뿐이고 실제 검사는 RateLimitInterceptor가 수행한다.
 * (@AdminOnly + AdminOnlyInterceptor와 같은 구조)
 *
 * <p>[적용 대상] 비인증(permitAll)으로 열려 있어 로그인 없이 누구나 반복 호출 가능한
 * 공개 API 위주. 로그인 필요 API는 사용자 단위 어뷰징을 다른 수단(계정 정지 등)으로
 * 다룰 수 있어 우선순위가 낮다.
 *
 * <p>[향후 확장] 인스턴스가 여러 대로 늘어나면 인메모리 버킷 저장소를 Redis 기반으로
 * 교체해야 한다 — 인터셉터 내부 저장소만 바꾸면 되므로 어노테이션/컨트롤러는 그대로 둔다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 버킷 용량(최대 토큰 수) — 짧은 순간에 허용할 최대 요청 수 */
    int capacity() default 5;

    /** capacity만큼의 토큰을 다시 채우는 데 걸리는 시간(초) */
    int refillSeconds() default 60;
}
