package com.example.toget.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record FundingConfirmSettlementResponse(
        @Schema(description = "정산 확인된 펀딩 ID", example = "12")
        Long fundingId
) {}
