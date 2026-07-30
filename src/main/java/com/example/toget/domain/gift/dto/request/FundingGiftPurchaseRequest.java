package com.example.toget.domain.gift.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record FundingGiftPurchaseRequest(
        @Schema(description = "구매 링크")
        @NotBlank(message = "구매링크는 필수입니다.")
        String purchaseUrl,

        @Schema(description = "영수증/구매 인증 이미지")
        @NotBlank(message = "이미지는 필수입니다.")
        String receiptImageUrl
) {}