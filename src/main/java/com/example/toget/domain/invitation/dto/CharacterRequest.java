package com.example.toget.domain.invitation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 캐릭터 생성(POST) / 수정(PUT) 요청 본문.
 * PUT은 리소스를 전체 교체하는 방식이므로 생성과 동일한 필수 필드를 사용하여 하나의 DTO를 공용으로 사용한다.
 */
public record CharacterRequest(
        @Schema(description = "캐릭터 이름", example = "토끼")
        @NotBlank(message = "캐릭터 이름은 필수입니다.")
        @Size(max = 50, message = "캐릭터 이름은 50자를 초과할 수 없습니다.")
        String name,

        @Schema(description = "캐릭터 이미지 URL", example = "https://toget.com/images/rabbit.png")
        @NotBlank(message = "이미지 URL은 필수입니다.")
        @Size(max = 2048, message = "이미지 URL은 2048자를 초과할 수 없습니다.")
        @Pattern(regexp = "^https?://.+", message = "이미지 URL은 http:// 또는 https://로 시작하는 유효한 URL이어야 합니다.")
        String imageUrl
) {
}
