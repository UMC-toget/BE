package com.example.toget.domain.gift.dto.response;


import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record FundingGiftCandidateDetailResponse(
        Long fundingGiftId,
        String giftImageUrl,
        String giftName,
        Long giftPrice,
        long voteCount,
        String giftPurchaseUrl,
        String registrantName,
        String note,
        boolean isVotedByViewer,
        List<CommentItem> comments
) {
    public record CommentItem(
            Long commentId,
            String userName,
            String profileImageUrl,
            String content
    ) {}
}