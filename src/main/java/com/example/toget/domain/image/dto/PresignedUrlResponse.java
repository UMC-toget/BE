package com.example.toget.domain.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PresignedUrlResponse(
        @Schema(description = "S3 Direct Upload용 Presigned PUT URL", example = "https://toget-bucket-dev.s3.ap-northeast-2.amazonaws.com/...")
        String presignedUrl,

        @Schema(description = "업로드 완료 후 접근 가능한 최종 S3 이미지 URL", example = "https://toget-bucket-dev.s3.ap-northeast-2.amazonaws.com/profiles/...")
        String imageUrl
) {
}
