package com.example.toget.domain.funding.dto.request;

import com.example.toget.domain.funding.enums.SettlementStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record FundingSettlementStatusUpdateRequest(
        @Schema(description = "변경할 입금 상태 (CONFIRMED 또는 PAID로 되돌리기)", example = "CONFIRMED")
        @NotNull(message = "입금 상태는 필수입니다.")
        SettlementStatus settlementStatus
) {}