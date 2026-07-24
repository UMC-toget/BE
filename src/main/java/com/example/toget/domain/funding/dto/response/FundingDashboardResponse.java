package com.example.toget.domain.funding.dto.response;

import java.time.LocalDate;
import java.util.List;

public record FundingDashboardResponse(

        Long fundingId,

        String title,

        String recipientName,

        LocalDate anniversaryDate,

        LocalDate startDate,

        LocalDate endDate,

        String introduction,

        Long targetAmount,

        Long collectedAmount,

        Double progressRate,

        String status,

        UserAccountResponse userAccount,

        VisibilityResponse visibility,

        List<GiftResponse> gifts
) {

    public record UserAccountResponse(
            Long userAccountId,
            String bankCode,
            String accountHolder,
            String accountNumber
    ) {}

    public record VisibilityResponse(
            Boolean isProgressVisible,
            Boolean isParticipantCountVisible,
            Boolean isParticipantNameVisible,
            Boolean isMessageVisible,
            Boolean isCollectedAmountVisible
    ) {}

    public record GiftResponse(
            Long fundingGiftId,
            String giftName,
            Long giftPrice,
            String giftPurchaseUrl,
            String giftImageUrl
    ) {}
}