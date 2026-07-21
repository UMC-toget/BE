package com.example.toget.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 프로필 수정 응답 — 수정 결과가 반영된 최종 값을 돌려줘 프론트가 재조회 없이 화면을 갱신할 수 있게 한다 */
public record UserProfileUpdateResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "수정된 닉네임", example = "새로운닉네임")
        String nickname,

        @Schema(description = "수정된 프로필 이미지 URL", example = "https://toget.com/images/new_profile.png")
        String profileImageUrl
) {
}
