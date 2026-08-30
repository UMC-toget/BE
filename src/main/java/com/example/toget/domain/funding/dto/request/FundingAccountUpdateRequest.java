package com.example.toget.domain.funding.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record FundingAccountUpdateRequest(
        @Schema(description = "변경할 계좌 ID", example = "4")
        @NotNull(message = "계좌 ID는 필수입니다.")
        Long userAccountId
) {}