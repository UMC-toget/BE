package com.example.toget.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record FundingContributionCreateResponse(
        @Schema(description = "펀딩 ID") Long fundingId
) {}