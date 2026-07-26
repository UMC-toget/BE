package com.example.toget.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record FundingContributionDetailResponse(
        @Schema(description = "참여 ID") Long contributionId,
        @Schema(description = "편지 내용 (비공개면 null)") String content
) {}