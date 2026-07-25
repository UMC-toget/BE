package com.example.toget.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public record FundingBasicInfoResponse(
        @Schema(description = "펀딩 ID")
        Long fundingId,

        @Schema(description = "제목")
        String title,

        @Schema(description = "기념일")
        LocalDate anniversaryDate,

        @Schema(description = "시작일")
        LocalDate startDate,

        @Schema(description = "종료일")
        LocalDate endDate,

        @Schema(description = "소개글")
        String introduction,

        @Schema(description = "대표 이미지 URL")
        String thumbnailImageUrl
) {}