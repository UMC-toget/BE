package com.example.toget.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

/** POST /api/v1/auth/tokens/refresh 요청 본문 — 재발급에 사용할 refresh token */
public record TokenRefreshRequest(
        @NotBlank(message = "refreshToken은 필수입니다.")
        String refreshToken
) {
}
