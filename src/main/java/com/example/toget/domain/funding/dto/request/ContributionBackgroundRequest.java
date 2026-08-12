package com.example.toget.domain.funding.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 배경 색상 생성/수정 공용 요청 DTO */
public record ContributionBackgroundRequest(

        @Schema(description = "배경 색상 이름", example = "파스텔 핑크")
        @NotBlank(message = "배경 색상 이름은 필수입니다.")
        @Size(max = 50, message = "배경 색상 이름은 50자를 초과할 수 없습니다.")
        String name,

        @Schema(description = "배경 색상 HEX 코드", example = "#FFB6C1")
        @NotBlank(message = "HEX 코드는 필수입니다.")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "올바른 HEX 코드 형식이 아닙니다. (예: #FFB6C1)")
        String hexCode,

        @Schema(description = "원색 HEX 코드", example = "#FF007F")
        @NotBlank(message = "원색 HEX 코드는 필수입니다.")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "올바른 HEX 코드 형식이 아닙니다. (예: #FF007F)")
        String solidColorHex
) {}