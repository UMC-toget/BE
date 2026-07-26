package com.example.toget.domain.funding.dto.response;

import java.time.LocalDate;
import java.util.List;

public record FundingTogetherGiftDashboardResponse(
        Long fundingId,
        String status,
        LocalDate anniversaryDate,
        String recipientName,
        String introduction,
        String thumbnailImageUrl,
        List<MemberSummary> members,
        List<TopGift> topGifts,
        Long collectedAmount,
        Long targetAmount,
        List<ConfirmedGift> confirmedGifts,
        List<Long> messageIds
) {
    public record MemberSummary(Long fundingMemberId, Long userId, String name, String profileImageUrl, String role) {}
    public record TopGift(Long fundingGiftId, String giftName, Long giftPrice, String giftImageUrl, long voteCount) {}
    public record ConfirmedGift(Long fundingGiftId, String giftName, Long giftPrice, String giftImageUrl) {}
}