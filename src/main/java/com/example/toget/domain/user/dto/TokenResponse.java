package com.example.toget.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 토큰 재발급 응답.
 * Rotation 정책이라 access뿐 아니라 refresh도 매번 새로 발급된다
 * — 클라이언트는 두 토큰 모두 갈아끼워야 한다.
 */
public record TokenResponse(
        @Schema(description = "새로 발급된 Access Token (만료 1시간)", example = "eyJhbGciOiJSUzI1NiIs...")
        String accessToken,

        @Schema(description = "새로 발급된 Refresh Token (만료 14일)", example = "eyJhbGciOiJSUzI1NiIs...")
        String refreshToken
) {
}
