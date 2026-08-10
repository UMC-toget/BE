package com.example.toget.domain.funding.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 내가 개최한 선물 준비 목록 조회 응답.
 * 전체 개수(totalCount)는 명세에 없으므로 담지 않는다 — Slice 기반 hasNext만 제공.
 */
public record MyFundingListResponse(
        @Schema(description = "펀딩 목록")
        List<MyFundingSummary> fundings,

        @Schema(description = "현재 페이지 번호(0부터 시작)", example = "0")
        int currentPage,

        @Schema(description = "페이지 크기", example = "10")
        int pageSize,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {

    /**
     * 목록의 개별 펀딩 항목.
     * fundingType/status는 UserConverter의 oauthProvider와 같은 방식으로 enum name 문자열로 내린다.
     */
    public record MyFundingSummary(
            @Schema(description = "펀딩 ID", example = "12")
            Long fundingId,

            @Schema(description = "펀딩 유형", example = "MY_GIFT")
            String fundingType,   // "MY_GIFT" / "TOGETHER_GIFT"

            @Schema(description = "제목", example = "OO의 생일 선물")
            String title,

            @Schema(description = "받는 사람", example = "홍길동")
            String recipientName,

            @Schema(description = "목표 금액", example = "500000")
            Long targetAmount,

            @Schema(description = "참여금 합계, 없으면 0", example = "320000")
            Long collectedAmount,

            @Schema(description = "달성률(%), 내림, targetAmount 0 이면 0", example = "64.0")
            double progressRate,

            @Schema(description = "진행 상태", example = "SELECTING")
            String status,        // "SELECTING" / "SETTLING" / "ENDED"

            @Schema(description = "종료일", example = "2026-07-31")
            LocalDate endDate,

            @Schema(description = "대표 이미지 URL", example = "https://cdn.toget.com/images/thumbnail.png")
            String thumbnailImageUrl,

            @Schema(description = "생성 일시", example = "2026-06-01T10:00:00")
            LocalDateTime createdAt,

            @Schema(description = "후기 작성 여부", example = "true")
            boolean hasReview
    ) {
    }
}
