package com.example.toget.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/**
 * 외부 방문자용 선물 준비 상세 조회 응답 DTO.
 *
 * [설계 포인트]
 *  - 개설자용 FundingMyGiftDashboardResponse와 필드명·순서를 동일하게 맞췄다.
 *    두 화면(E02 개설자 뷰 / 비개설자 뷰)이 레이아웃을 공유해 프론트가 컴포넌트를 재사용하기 때문.
 *  - 다만 정산 계좌(userAccount)는 제외한다. 외부에 노출되면 안 되는 정보다.
 *  - collectedAmount / progressRate / participantCount는 공개 설정에 따라 null이 될 수 있어
 *    원시 타입(int) 대신 래퍼 타입(Integer)을 쓴다. 개설자용은 항상 값이 있어 int를 쓰는 것과 다르다.
 */
public record SharedFundingDetailResponse(

        @Schema(description = "펀딩 ID", example = "12")
        Long fundingId,

        @Schema(description = "펀딩 제목", example = "희주의 25번째 생일")
        String title,

        @Schema(description = "선물 받는 사람 이름", example = "김희주")
        String recipientName,

        @Schema(description = "기념일", example = "2026-03-15")
        LocalDate anniversaryDate,

        @Schema(description = "펀딩 시작일", example = "2026-02-20")
        LocalDate startDate,

        @Schema(description = "펀딩 종료일", example = "2026-03-20")
        LocalDate endDate,

        @Schema(description = "소개글")
        String introduction,

        @Schema(description = "대표 이미지 URL")
        String thumbnailImageUrl,

        @Schema(description = "목표 금액", example = "392000")
        Long targetAmount,

        @Schema(description = "모인 금액. showAmount=false면 null", example = "203800")
        Long collectedAmount,

        @Schema(description = "달성률(%). showProgress=false면 null", example = "52.0")
        Double progressRate,

        @Schema(description = "참여자 수. showParticipantCount=false면 null", example = "7")
        Integer participantCount,

        @Schema(description = "진행 상태. MY_GIFT는 SETTLING(진행 중) / ENDED(마감) 두 값만 존재", example = "SETTLING")
        String status,

        @Schema(description = "개설자가 지정한 공개 범위 설정")
        VisibilityInfo visibility,

        @Schema(description = "받고 싶은 선물 목록")
        List<GiftInfo> gifts

) {
    /**
     * 공개 범위 플래그.
     * 값 자체는 이미 서버에서 null 처리되지만, 프론트가 "비공개"와 "값 없음"을 구분해
     * 안내 문구를 띄울 수 있도록 함께 내려준다.
     */
    public record VisibilityInfo(
            Boolean showProgress, Boolean showAmount, Boolean showParticipantCount,
            Boolean showParticipantNames, Boolean showMessages
    ) {}

    public record GiftInfo(
            Long fundingGiftId, String giftName, Long giftPrice,
            String giftPurchaseUrl, String giftImageUrl
    ) {}
}
