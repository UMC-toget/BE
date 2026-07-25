package com.example.toget.global.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 관리자 전용 API임을 표시하는 어노테이션.
 * 컨트롤러 메서드(또는 클래스 전체)에 붙이면 AdminOnlyInterceptor가 요청자를 검사해
 * 설정된 관리자 계정이 아닐 경우 403(COMMON403_1)으로 차단한다.
 *
 * <p>어노테이션 자체는 "표시"일 뿐이고 실제 검사는 AdminOnlyInterceptor가 수행한다.
 * (@LoginUserId + LoginUserIdArgumentResolver와 같은 구조)
 *
 * <p>[왜 ArgumentResolver가 아니라 Interceptor인가]
 * ArgumentResolver는 해당 어노테이션이 붙은 "파라미터"가 있어야만 동작하므로,
 * 파라미터와 무관하게 메서드 단위로 걸어야 하는 권한 검사에는 맞지 않는다.
 *
 * <p>[향후 확장] 관리자가 여러 명이 되거나 등급 구분이 필요해지면
 * users 테이블에 role 컬럼을 추가하고 이 어노테이션의 판정 로직만 교체하면 된다.
 * 컨트롤러에 붙은 표시는 그대로 두어도 되므로 변경 범위가 좁다.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME) // 런타임에 리플렉션으로 존재를 확인해야 하므로 RUNTIME
public @interface AdminOnly {
}
