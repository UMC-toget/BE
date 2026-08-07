package com.example.toget.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

public record FundingTogetherGiftDashboardResponse(
        @Schema(description = "펀딩 ID", example = "12")
        Long fundingId,

        @Schema(description = "진행 상태", example = "SELECTING")
        String status,

        @Schema(description = "조회자의 이 펀딩 내 역할 (CREATOR/ADMIN/PARTICIPANT). "
                + "비회원이거나 아직 합류하지 않은 회원이면 null — 프론트는 이 값으로 액션 버튼 활성화 여부를 결정한다.",
                example = "PARTICIPANT")
        String myRole,

        @Schema(description = "필요 날짜", example = "2026-08-15")
        LocalDate anniversaryDate,

        @Schema(description = "받는 사람", example = "홍길동")
        String recipientName,

        @Schema(description = "준비방 메모", example = "생일을 함께 축하해주세요!")
        String introduction,

        @Schema(description = "대표 이미지 URL", example = "https://cdn.toget.com/images/thumbnail.png")
        String thumbnailImageUrl,

        @Schema(description = "참여 멤버 목록")
        List<MemberSummary> members,

        @Schema(description = "인기 선물 목록")
        List<TopGift> topGifts,

        @Schema(description = "현재까지 모인 총 금액", example = "320000")
        Long collectedAmount,

        @Schema(description = "목표 금액", example = "500000")
        Long targetAmount,

        @Schema(description = "확정된 선물 목록")
        List<ConfirmedGift> confirmedGifts,

        @Schema(description = "롤링페이퍼 메시지 ID 목록", example = "[1, 2, 3]")
        List<Long> messageIds
) {
    public record MemberSummary(
            @Schema(description = "펀딩 멤버 ID", example = "3")
            Long fundingMemberId,

            @Schema(description = "사용자 ID", example = "8")
            Long userId,

            @Schema(description = "이름", example = "김민수")
            String name,

            @Schema(description = "프로필 이미지 URL", example = "https://cdn.toget.com/images/profile.png")
            String profileImageUrl,

            @Schema(description = "역할", example = "HOST")
            String role
    ) {}

    public record TopGift(
            @Schema(description = "펀딩 선물 ID", example = "10")
            Long fundingGiftId,

            @Schema(description = "선물 이름", example = "플레이스테이션 5")
            String giftName,

            @Schema(description = "선물 가격", example = "628000")
            Long giftPrice,

            @Schema(description = "선물 이미지 URL", example = "https://cdn.toget.com/images/ps5.png")
            String giftImageUrl,

            @Schema(description = "투표 수", example = "5")
            long voteCount
    ) {}

    public record ConfirmedGift(
            @Schema(description = "펀딩 선물 ID", example = "10")
            Long fundingGiftId,

            @Schema(description = "선물 이름", example = "플레이스테이션 5")
            String giftName,

            @Schema(description = "선물 가격", example = "628000")
            Long giftPrice,

            @Schema(description = "선물 이미지 URL", example = "https://cdn.toget.com/images/ps5.png")
            String giftImageUrl
    ) {}
}
