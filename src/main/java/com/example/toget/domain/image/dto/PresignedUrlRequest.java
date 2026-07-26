package com.example.toget.domain.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PresignedUrlRequest(
        @Schema(description = "저장할 디렉토리/프리픽스 (예: profiles, fundings)", example = "profiles")
        @Size(max = 255, message = "프리픽스는 255자 이하이어야 합니다.")
        String prefix,

        @Schema(description = "원래 파일명 (예: avatar.png)", example = "avatar.png")
        @NotBlank(message = "파일명은 필수 입력값입니다.")
        @Size(max = 255, message = "파일명은 255자 이하이어야 합니다.")
        String fileName,

        @Schema(description = "MIME 타입 (예: image/png, image/jpeg)", example = "image/png")
        @NotBlank(message = "MIME 타입은 필수 입력값입니다.")
        @Size(max = 255, message = "MIME 타입은 255자 이하이어야 합니다.")
        String contentType
) {
}
