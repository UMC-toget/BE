package com.example.toget.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * POST /api/v1/auth/tokens/{provider} 요청 본문.
 * identityToken: 프론트가 소셜 SDK로 받은 토큰 (카카오=access token, 구글=ID token).
 * @NotBlank: null·빈 문자열·공백만 있는 값 거부 — 컨트롤러의 @Valid가 트리거하며 실패 시 400.
 */
public record SocialLoginRequest(
        @NotBlank(message = "identityToken은 필수입니다.")
        String identityToken
) {
}
