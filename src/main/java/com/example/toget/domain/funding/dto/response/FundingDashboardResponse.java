package com.example.toget.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

public record FundingDashboardResponse(
        @Schema(description = "펀딩 ID", example = "12")
        Long id,

        @Schema(description = "펀딩 유형", example = "MY_GIFT")
        String fundingType,

        @Schema(description = "진행 상태", example = "SELECTING")
        String status,

        @Schema(description = "종료 결정 필요 여부 (SELECTING 상태에서만 의미 있음)", example = "false")
        boolean needsEndDecision,

        @Schema(description = "제목", example = "OO의 생일 선물")
        String title,

        @Schema(description = "받는 사람", example = "홍길동")
        String recipientName,

        @Schema(description = "필요 날짜", example = "2026-08-15")
        LocalDate anniversaryDate,

        @Schema(description = "시작일", example = "2026-07-01")
        LocalDate startDate,

        @Schema(description = "종료일", example = "2026-07-31")
        LocalDate endDate,

        @Schema(description = "준비방 메모", example = "생일을 함께 축하해주세요!")
        String introduction,

        @Schema(description = "대표 이미지 URL", example = "https://cdn.toget.com/images/thumbnail.png")
        String thumbnailImageUrl,

        @Schema(description = "목표 금액", example = "500000")
        Long targetAmount,

        @Schema(description = "현재까지 모인 총 금액", example = "320000")
        Long collectedAmount,

        @Schema(description = "참여자 수", example = "14")
        int participantCount,

        @Schema(description = "정산 계좌 정보 (미등록 시 null)")
        AccountInfo account,

        @Schema(description = "공개 설정 (TOGETHER_GIFT는 null)")
        VisibilityInfo visibility,

        @Schema(description = "초대장 정보")
        InvitationInfo invitation,

        @Schema(description = "선물 목록")
        List<GiftInfo> gifts
) {
    public record AccountInfo(
            @Schema(description = "사용자 계좌 ID", example = "5")
            Long userAccountId,

            @Schema(description = "은행명", example = "카카오뱅크")
            String bankName,

            @Schema(description = "계좌번호", example = "3333-12-3456789")
            String bankAccount,

            @Schema(description = "예금주", example = "홍길동")
            String accountOwner
    ) {}

    public record VisibilityInfo(
            @Schema(description = "진행률 공개 여부", example = "true")
            Boolean isProgressVisible,

            @Schema(description = "참여자 수 공개 여부", example = "true")
            Boolean isParticipantCountVisible,

            @Schema(description = "참여자 이름 공개 여부", example = "false")
            Boolean isParticipantNameVisible,

            @Schema(description = "축하 메시지 공개 여부", example = "true")
            Boolean isMessageVisible,

            @Schema(description = "모금액 공개 여부", example = "true")
            Boolean isCollectedAmountVisible
    ) {}

    public record InvitationInfo(
            @Schema(description = "캐릭터 ID", example = "1")
            Long characterId,

            @Schema(description = "배경 ID", example = "2")
            Long backgroundId,

            @Schema(description = "초대장 제목", example = "생일 파티에 초대합니다!")
            String title,

            @Schema(description = "초대장 내용", example = "함께 모여 즐거운 시간을 보내요!")
            String content,

            @Schema(description = "공유용 초대장 URL", example = "https://toget.com/invitations/abc123")
            String url
    ) {}

    public record GiftInfo(
            @Schema(description = "펀딩 선물 ID", example = "10")
            Long fundingGiftId,

            @Schema(description = "선물 이름", example = "플레이스테이션 5")
            String giftName,

            @Schema(description = "선물 가격", example = "628000")
            Long giftPrice,

            @Schema(description = "구매처 URL", example = "https://store.playstation.com/")
            String giftPurchaseUrl,

            @Schema(description = "선물 이미지 URL", example = "https://cdn.toget.com/images/ps5.png")
            String giftImageUrl,

            @Schema(description = "선물 상태", example = "SELECTING")
            String status
    ) {}
}