package com.example.toget.domain.gift.dto.response;


public record FundingGiftResponse(
        Long fundingGiftId,
        String giftName,
        Long giftPrice,
        String giftPurchaseUrl,
        String giftImageUrl
) {}