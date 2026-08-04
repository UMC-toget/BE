package com.example.toget.domain.funding.dto.response;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

public record FundingMyGiftDashboardResponse(
        @Schema(description = "펀딩 ID", example = "12")
        Long fundingId,

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

        @Schema(description = "달성률(%)", example = "64.0")
        Double progressRate,

        @Schema(description = "참여자 수", example = "14")
        int participantCount,

        @Schema(description = "진행 상태", example = "SELECTING")
        String status,

        @Schema(description = "정산 계좌 정보 (미등록 시 null)")
        AccountInfo userAccount,

        @Schema(description = "공개 설정")
        VisibilityInfo visibility,

        @Schema(description = "선물 목록")
        List<GiftInfo> gifts
) {
    public record AccountInfo(
            @Schema(description = "사용자 계좌 ID", example = "5")
            Long userAccountId,

            // 실제로 내려가는 값은 표시명이 아니라 BankName enum의 이름 문자열이다.
            // (기존 example이 "카카오뱅크"로 되어 있어 실제 응답과 어긋나 있었다 — 이번에 바로잡음)
            @Schema(description = "은행 코드", example = "KAKAO_BANK")
            String bankName,

            @Schema(description = "은행 표시명", example = "카카오뱅크")
            String bankDisplayName,

            @Schema(description = "은행 아이콘 URL — 아이콘 미확보 은행은 null이므로 fallback 처리 필요",
                    example = "https://toget-bucket-dev.s3.ap-northeast-2.amazonaws.com/bank-icons/v1/KAKAO_BANK.svg")
            String bankIconUrl,

            @Schema(description = "계좌번호", example = "3333-12-3456789")
            String account,

            @Schema(description = "예금주", example = "홍길동")
            String accountOwner
    ) {}

    public record VisibilityInfo(
            @Schema(description = "진행률 공개 여부", example = "true")
            Boolean showProgress,

            @Schema(description = "모금액 공개 여부", example = "true")
            Boolean showAmount,

            @Schema(description = "참여자 수 공개 여부", example = "true")
            Boolean showParticipantCount,

            @Schema(description = "참여자 이름 공개 여부", example = "false")
            Boolean showParticipantNames,

            @Schema(description = "축하 메시지 공개 여부", example = "true")
            Boolean showMessages
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
            String giftImageUrl
    ) {}
}
