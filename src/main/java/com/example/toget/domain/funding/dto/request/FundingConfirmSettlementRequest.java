package com.example.toget.domain.funding.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record FundingConfirmSettlementRequest(
        @Schema(description = "확정할 선물 후보 ID 목록")
        @NotEmpty(message = "확정할 선물을 최소 1개 이상 선택해야 합니다.")
        List<Long> giftIds,

        @Schema(description = "정산 참여자로 확정할 fundingMemberId 목록")
        @NotEmpty(message = "정산 참여자를 최소 1명 이상 선택해야 합니다.")
        List<Long> settlementMemberIds
) {}