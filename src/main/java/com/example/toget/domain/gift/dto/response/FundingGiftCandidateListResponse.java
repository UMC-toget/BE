package com.example.toget.domain.gift.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record FundingGiftCandidateListResponse(
        @Schema(description = "요청자가 투표한 후보 ID 목록 (최대 3개)") List<Long> votedGiftIds,
        @Schema(description = "후보 목록") List<CandidateItem> candidates
) {
    public record CandidateItem(
            Long fundingGiftId,
            String giftImageUrl,
            String giftName,
            Long giftPrice,
            long voteCount
    ) {}
}