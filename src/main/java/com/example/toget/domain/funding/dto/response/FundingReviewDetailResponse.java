package com.example.toget.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record FundingReviewDetailResponse(
        @Schema(description = "펀딩 후기 ID", example = "7")
        Long fundingReviewId,

        @Schema(description = "후기 유형", example = "TOGETHER_GIFT")
        String type,

        @Schema(description = "후기 제목", example = "다들 고마워요!")
        String title,

        @Schema(description = "후기 내용", example = "덕분에 좋은 선물을 준비할 수 있었어요.")
        String content,

        @Schema(description = "배경 ID", example = "3")
        Long backgroundId,

        @Schema(description = "첨부 이미지 URL 목록", example = "[\"https://cdn.toget.com/images/review1.png\"]")
        List<String> images,

        @Schema(description = "작성 일시", example = "2026-08-01T10:00:00")
        LocalDateTime createdAt
) {}
