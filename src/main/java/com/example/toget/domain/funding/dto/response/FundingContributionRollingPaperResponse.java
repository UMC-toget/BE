package com.example.toget.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record FundingContributionRollingPaperResponse(
        @Schema(description = "참여자 수") int participantCount,
        @Schema(description = "축하 메시지 목록") List<ContributionItem> contributions
) {
    public record ContributionItem(
            Long contributionId,
            String senderName,
            Boolean isAnonymous,
            Long amount,
            String content,
            Boolean isPrivate,
            @Schema(description = "편지지 배경 ID (1~8)") Long backgroundId,
            LocalDateTime createdAt
    ) {}
}