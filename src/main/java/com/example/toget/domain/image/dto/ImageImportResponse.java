package com.example.toget.domain.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ImageImportResponse(

        @Schema(description = "우리 S3에 저장된 최종 이미지 URL — 이 값을 프로필/위시/선물에 저장한다",
                example = "https://toget-bucket-dev.s3.ap-northeast-2.amazonaws.com/profiles/9f1c....jpg")
        String imageUrl
) {
}
