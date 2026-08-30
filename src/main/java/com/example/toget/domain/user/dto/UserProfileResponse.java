package com.example.toget.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 내 프로필 조회 응답.
 * User 엔티티에서 공개 가능한 필드만 골라 담는다 — refreshToken, status 같은 내부 상태는 제외.
 */
public record UserProfileResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "사용자 이메일", example = "user@example.com")
        String email,

        @Schema(description = "사용자 닉네임", example = "투겟러브")
        String nickname,

        @Schema(description = "사용자 실명", example = "홍길동")
        String name,

        @Schema(description = "프로필 이미지 URL", example = "https://toget.com/images/profile.png")
        String profileImageUrl,

        @Schema(description = "소셜 로그인 제공자 (KAKAO / GOOGLE / APPLE)", example = "KAKAO")
        String oauthProvider // "KAKAO" / "GOOGLE" — 어느 소셜 계정으로 가입했는지 표시용
) {
}
