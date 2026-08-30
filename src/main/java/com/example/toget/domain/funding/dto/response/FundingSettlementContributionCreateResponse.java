package com.example.toget.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record FundingSettlementContributionCreateResponse(
        @Schema(description = "펀딩 ID") Long fundingId,
        @Schema(description = "펀딩 멤버 ID") Long memberId,
        @Schema(description = "정산 입금 상태", example = "PAID") String settlementStatus
) {}
