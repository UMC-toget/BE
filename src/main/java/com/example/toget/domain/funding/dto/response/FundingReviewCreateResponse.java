package com.example.toget.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record FundingReviewCreateResponse(
        @Schema(description = "생성된 펀딩 후기 ID", example = "7")
        Long fundingReviewId
) {}
