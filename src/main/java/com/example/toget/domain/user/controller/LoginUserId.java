package com.example.toget.domain.user.controller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 붙이면 현재 로그인한 사용자의 ID(Long)가 자동 주입되는 커스텀 어노테이션.
 * 사용 예: {@code public ApiResponse<...> getMyProfile(@LoginUserId Long userId)}
 *
 * 어노테이션 자체는 "표시"일 뿐이고, 실제 값 주입은 LoginUserIdArgumentResolver가 수행한다.
 */
@Target(ElementType.PARAMETER)      // 메서드 파라미터에만 붙일 수 있음
@Retention(RetentionPolicy.RUNTIME) // 런타임에도 유지 → 리플렉션으로 어노테이션 존재를 확인 가능
public @interface LoginUserId {
}
