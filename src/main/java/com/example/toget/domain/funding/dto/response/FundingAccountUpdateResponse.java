package com.example.toget.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record FundingAccountUpdateResponse(
        @Schema(description = "펀딩 ID")
        Long fundingId,

        @Schema(description = "변경된 계좌 ID")
        Long userAccountId
) {}