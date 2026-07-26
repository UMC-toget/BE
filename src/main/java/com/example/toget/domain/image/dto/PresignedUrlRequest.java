package com.example.toget.domain.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record PresignedUrlRequest(
        @Schema(description = "저장할 디렉토리/프리픽스 (예: profiles, fundings)", example = "profiles")
        String prefix,

        @Schema(description = "원래 파일명 (예: avatar.png)", example = "avatar.png")
        @NotBlank(message = "파일명은 필수 입력값입니다.")
        String fileName,

        @Schema(description = "MIME 타입 (예: image/png, image/jpeg)", example = "image/png")
        @NotBlank(message = "MIME 타입은 필수 입력값입니다.")
        String contentType
) {
}
