package com.example.toget.global.util;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 현재 요청의 인증 주체(principal)에서 로그인 사용자 ID를 안전하게 꺼내는 유틸.
 *
 * [주의] 만료·위조 토큰은 여기까지 오지 않는다. JwtAuthenticationFilter가 먼저 401로 끊기 때문에,
 * 이 메서드가 null을 돌려준다면 그건 "토큰을 아예 안 보낸 진짜 비회원"이라는 뜻이다.
 */
public final class AuthenticatedUserUtils {

    /** 인스턴스를 만들 이유가 없는 정적 유틸이므로 생성자를 막는다 */
    private AuthenticatedUserUtils() {
    }

    /**
     * 로그인 사용자의 ID를 반환한다. 비로그인 요청이면 null.
     *
     * @return 로그인 회원의 userId, 비회원이면 null
     */
    public static Long getCurrentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        // 익명 인증 걸러내기.
        // 이슈 본문 스케치는 principal을 "anonymousUser" 문자열과 비교했지만, 그 값은
        // AnonymousAuthenticationFilter의 기본값일 뿐 커스터마이징이 가능하다.
        // 인증 객체의 타입으로 판정하면 principal을 무엇으로 바꾸든 안전하다.
        if (authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        // 타입 검증: JwtAuthenticationFilter가 넣은 Long이 아니면 userId로 오인하지 않는다.
        // (다른 인증 방식이 UserDetails나 문자열을 principal에 넣더라도 비회원으로 취급)
        if (!(authentication.getPrincipal() instanceof Long userId)) {
            return null;
        }
        return userId;
    }

    /**
     * 로그인 여부만 알면 될 때 쓰는 편의 메서드.
     *
     * @return 로그인 회원이면 true
     */
    public static boolean isAuthenticated() {
        return getCurrentUserIdOrNull() != null;
    }
}
