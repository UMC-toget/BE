package com.example.toget.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record FundingSettlementListResponse(
        @Schema(description = "정산 참여자 수")
        int settlementParticipantCount,

        @Schema(description = "총 정산 금액")
        Long totalSettlementAmount,

        @Schema(description = "정산 참여자별 상세")
        List<SettlementInfo> settlements
) {
    public record SettlementInfo(
            Long fundingMemberId,
            Long userId,
            String name,
            String profileImageUrl,
            Long amountDue,
            String settlementStatus
    ) {}
}