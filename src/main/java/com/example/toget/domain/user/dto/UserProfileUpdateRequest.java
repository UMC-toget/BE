package com.example.toget.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 프로필 수정(PATCH) 요청 본문.
 * @NotBlank가 없는 이유: PATCH는 "보낸 필드만 수정"이므로 null(=안 보냄)을 허용해야 한다.
 * null 필드는 User.updateProfile에서 건너뛴다. @Size는 값이 있을 때만 검사된다.
 */
public record UserProfileUpdateRequest(
        @Schema(description = "변경할 닉네임", example = "새로운닉네임")
        @Size(min = 1, max = 6, message = "닉네임은 1자 이상 6자 이하여야 합니다.")
        String nickname,

        @Schema(description = "변경할 프로필 이미지 URL", example = "https://toget.com/images/new_profile.png")
        @Size(max = 512, message = "프로필 이미지 URL은 512자 이하여야 합니다.")
        String profileImageUrl
) {
}
