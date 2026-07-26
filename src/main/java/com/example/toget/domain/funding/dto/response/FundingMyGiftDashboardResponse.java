package com.example.toget.domain.funding.dto.response;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

public record FundingMyGiftDashboardResponse(
        Long fundingId,
        String title,
        String recipientName,
        LocalDate anniversaryDate,
        LocalDate startDate,
        LocalDate endDate,
        String introduction,
        String thumbnailImageUrl,
        Long targetAmount,
        Long collectedAmount,
        Double progressRate,
        int participantCount,
        String status,
        AccountInfo userAccount,
        VisibilityInfo visibility,
        List<GiftInfo> gifts
) {
    public record AccountInfo(Long userAccountId, String bankName, String account, String accountOwner) {}

    public record VisibilityInfo(
            Boolean showProgress, Boolean showAmount, Boolean showParticipantCount,
            Boolean showParticipantNames, Boolean showMessages
    ) {}

    public record GiftInfo(Long fundingGiftId, String giftName, Long giftPrice, String giftPurchaseUrl, String giftImageUrl) {}
}