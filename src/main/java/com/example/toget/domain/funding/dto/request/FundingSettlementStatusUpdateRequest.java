package com.example.toget.domain.funding.dto.request;

import com.example.toget.domain.funding.enums.SettlementStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record FundingSettlementStatusUpdateRequest(
        @Schema(
                description = "변경할 입금 상태. PAID→CONFIRMED(확인), CONFIRMED→PAID(되돌리기), " +
                        "PAID→UNPAID(미입금 처리)만 허용됩니다. CONFIRMED에서 UNPAID로 직접 변경은 불가합니다.",
                example = "UNPAID"
        )
        @NotNull(message = "입금 상태는 필수입니다.")
        SettlementStatus settlementStatus
) {}