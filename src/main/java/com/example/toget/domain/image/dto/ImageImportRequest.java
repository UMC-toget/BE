package com.example.toget.domain.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImageImportRequest(

        @Schema(description = "가져올 외부 이미지 URL — 보통 웹 사진 검색 결과의 imageUrl을 그대로 넣는다",
                example = "https://example.com/images/cake.jpg")
        @NotBlank(message = "이미지 URL은 필수 입력값입니다.")
        @Size(max = 2048, message = "이미지 URL은 2048자 이하여야 합니다.")
        String sourceImageUrl,

        @Schema(description = "저장할 디렉토리/프리픽스 (예: profiles, gifts). 미지정 시 web-images",
                example = "profiles")
        @Size(max = 255, message = "프리픽스는 255자 이하이어야 합니다.")
        String prefix
) {
}
