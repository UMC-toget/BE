package com.example.toget.domain.invitation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 초대장 배경 색상 생성(POST) / 수정(PUT) 요청 DTO.
 * * PUT은 리소스를 전체 교체하는 방식이므로 생성과 동일한 필수 필드를 사용하여 하나의 DTO를 공용으로 사용한다.
 */
public record InvitationBackgroundRequest(
        @Schema(description = "배경 색상 이름", example = "파스텔 옐로우")
        @NotBlank(message = "배경 색상 이름은 필수입니다.")
        @Size(max = 50, message = "배경 색상 이름은 50자를 초과할 수 없습니다.")
        String name,

        @Schema(description = "배경 색상 HEX 코드", example = "#FFFFE0")
        @NotBlank(message = "HEX 코드는 필수입니다.")
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "HEX 코드는 #RRGGBB 형식의 7자리 문자열이어야 합니다.")
        String hexCode,

        @Schema(description = "원색 HEX 코드", example = "#FFD700")
        @NotBlank(message = "원색 HEX 코드는 필수입니다.")
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "HEX 코드는 #RRGGBB 형식의 7자리 문자열이어야 합니다.")
        String solidColorHex
) {
}
