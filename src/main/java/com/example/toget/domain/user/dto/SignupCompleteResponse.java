package com.example.toget.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * POST /api/v1/users 응답 — 가입이 확정된 시점이라 여기서 처음으로 서비스 토큰이 발급된다.
 * 프론트는 이 토큰을 저장한 뒤 메인 화면으로 진입하면 된다. (issue #61)
 */
public record SignupCompleteResponse(
        @Schema(description = "생성된 사용자 고유 ID", example = "1")
        Long userId,

        @Schema(description = "사용자 이메일", example = "user@example.com")
        String email,

        @Schema(description = "사용자 이름 (소셜 프로필 기준)", example = "홍길동")
        String name,

        @Schema(description = "설정한 닉네임", example = "투겟러브")
        String nickname,

        @Schema(description = "설정한 프로필 이미지 URL", example = "https://toget.com/images/profile.png")
        String profileImageUrl,

        @Schema(description = "서비스 Access Token (만료 1시간)", example = "eyJhbGciOiJIUzI1NiIs...")
        String accessToken,

        @Schema(description = "서비스 Refresh Token (만료 14일)", example = "eyJhbGciOiJIUzI1NiIs...")
        String refreshToken
) {
}
