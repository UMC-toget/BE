package com.example.toget.domain.funding.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 내가 개최한 선물 준비 목록 조회 응답.
 * 전체 개수(totalCount)는 명세에 없으므로 담지 않는다 — Slice 기반 hasNext만 제공.
 */
public record MyFundingListResponse(
        List<MyFundingSummary> fundings,
        int currentPage,
        int pageSize,
        boolean hasNext
) {

    /**
     * 목록의 개별 펀딩 항목.
     * fundingType/status는 UserConverter의 oauthProvider와 같은 방식으로 enum name 문자열로 내린다.
     */
    public record MyFundingSummary(
            Long fundingId,
            String fundingType,   // "MY_GIFT" / "TOGETHER_GIFT"
            String title,
            String recipientName,
            Long targetAmount,
            Long collectedAmount, // 참여금 합계, 없으면 0
            String status,        // "SELECTING" / "SETTLING" / "ENDED"
            LocalDate endDate,
            String thumbnailImageUrl,
            LocalDateTime createdAt
    ) {
    }
}
