package com.example.toget.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record FundingGiftResponse(
        @Schema(description = "선물 ID") Long fundingGiftId,
        @Schema(description = "선물 이름") String giftName,
        @Schema(description = "선물 가격") Long giftPrice,
        @Schema(description = "구매처 URL") String giftPurchaseUrl,
        @Schema(description = "이미지 URL") String giftImageUrl
) {}