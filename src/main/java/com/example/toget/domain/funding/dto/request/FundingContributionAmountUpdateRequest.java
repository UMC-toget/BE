package com.example.toget.domain.funding.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record FundingContributionAmountUpdateRequest(
        @Schema(description = "수정할 후원 금액 (0원 이상)", example = "60000")
        @NotNull(message = "금액은 필수입니다.")
        @PositiveOrZero(message = "금액은 0원 이상이어야 합니다.")
        Long amount
) {}