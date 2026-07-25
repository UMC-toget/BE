package com.example.toget.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record FundingContributionAmountUpdateResponse(
        @Schema(description = "후원 기록 ID")
        Long contributionId,

        @Schema(description = "수정된 금액")
        Long amount
) {}