package com.example.toget.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record FundingContributionListResponse(
        @Schema(description = "전체 후원자 수") int participantCount,
        @Schema(description = "전체 후원 총액") Long totalAmount,
        @Schema(description = "이번 페이지 후원 기록") List<ContributionItem> contributions,
        @Schema(description = "현재 페이지") int currentPage,
        @Schema(description = "페이지 크기") int pageSize,
        @Schema(description = "다음 페이지 존재 여부") boolean hasNext
) {
    public record ContributionItem(
            Long contributionId,
            String senderName,
            String profileImageUrl,
            Long amount,
            LocalDateTime createdAt
    ) {}
}