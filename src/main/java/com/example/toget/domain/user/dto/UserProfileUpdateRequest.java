package com.example.toget.domain.user.dto;

import jakarta.validation.constraints.Size;

/**
 * 프로필 수정(PATCH) 요청 본문.
 * @NotBlank가 없는 이유: PATCH는 "보낸 필드만 수정"이므로 null(=안 보냄)을 허용해야 한다.
 * null 필드는 User.updateProfile에서 건너뛴다. @Size는 값이 있을 때만 검사된다.
 */
public record UserProfileUpdateRequest(
        @Size(max = 50, message = "닉네임은 50자 이하여야 합니다.")
        String nickname,

        @Size(max = 512, message = "프로필 이미지 URL은 512자 이하여야 합니다.")
        String profileImageUrl
) {
}
