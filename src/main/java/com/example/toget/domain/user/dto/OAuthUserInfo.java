package com.example.toget.domain.user.dto;

/**
 * 소셜 공급자에게 검증받아 온 사용자 정보.
 * 카카오/구글의 서로 다른 응답 구조를 이 공통 형태로 맞춰서(OAuthClient 구현체들이 변환)
 * AuthService는 공급자를 구분하지 않고 처리할 수 있다. oauthId 외에는 null 가능.
 */
public record OAuthUserInfo(
        String oauthId,
        String email,
        String name,
        String profileImageUrl
) {
}
