package com.example.toget.domain.workspace.dto;

import com.example.toget.global.enums.BankName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.time.LocalDate;
import java.util.List;

@Builder
public record IndividualFundingDraftDetailResponse(
        @Schema(description = "개인 펀딩 임시저장 선물 ID", example = "5")
        Long id,

        @Schema(description = "임시저장 작성 완료 단계", example = "1")
        Integer step,

        @Schema(description = "펀딩 제목", example = "내 생일 펀딩")
        String title,

        @Schema(description = "기념일 날짜", example = "2026-09-20")
        LocalDate anniversaryDate,

        @Schema(description = "펀딩 시작일", example = "2026-09-01")
        LocalDate startDate,

        @Schema(description = "펀딩 종료일", example = "2026-09-19")
        LocalDate endDate,

        @Schema(description = "감사 인사말/설명", example = "제 생일을 축하해주셔서 정말 감사합니다!")
        String greeting,

        @Schema(description = "펀딩 대표 썸네일 이미지 URL", example = "https://toget.com/images/thumbnail.png")
        String thumbnailUrl,

        @Schema(description = "정산용 등록 계좌 정보")
        AccountResponse account,

        @Schema(description = "공개 여부 설정 정보")
        VisibilitySettingsResponse visibilitySettings,

        @Schema(description = "초대장 카드 디자인 및 본문 정보")
        InvitationCardResponse invitationCard,

        @Schema(description = "임시 저장된 선물 목록")
        List<DraftGiftResponse> gifts
) {

    @Builder
    public record AccountResponse(
            @Schema(description = "사용자 등록 계좌 ID", example = "1")
            Long userAccountId,

            @Schema(description = "은행 이름", example = "KAKAO_BANK")
            BankName bankName,

            @Schema(description = "계좌 번호", example = "3333011234567")
            String bankAccount,

            @Schema(description = "예금주 명", example = "홍길동")
            String accountOwner
    ) {}

    @Builder
    public record VisibilitySettingsResponse(
            @Schema(description = "진행률 공개 여부", example = "true")
            Boolean isProgressPublic,

            @Schema(description = "모금액 공개 여부", example = "true")
            Boolean isAmountPublic,

            @Schema(description = "참여자 수 공개 여부", example = "true")
            Boolean isParticipantCountPublic,

            @Schema(description = "참여자 이름 공개 여부", example = "true")
            Boolean isParticipantNamePublic,

            @Schema(description = "축하 메시지 공개 여부", example = "true")
            Boolean isMessagePublic
    ) {}

    @Builder
    public record InvitationCardResponse(
            @Schema(description = "초대장에 사용된 캐릭터 ID", example = "2")
            Long characterId,

            @Schema(description = "초대장에 사용된 배경 ID", example = "3")
            Long backgroundId,

            @Schema(description = "초대장 제목", example = "생일파티에 초대합니다")
            String title,

            @Schema(description = "초대장 본문 내용", example = "맛있는 음식과 재미있는 게임을 함께 즐겨요!")
            String content
    ) {}

    @Builder
    public record DraftGiftResponse(
            @Schema(description = "선물 상품명", example = "아이패드 에어")
            String giftName,

            @Schema(description = "선물 가격", example = "850000")
            Long giftPrice,

            @Schema(description = "선물 구매처 링크 URL", example = "https://apple.com/ipad-air")
            String giftShopUrl,

            @Schema(description = "선물 이미지 URL", example = "https://toget.com/images/ipad.png")
            String giftImageUrl
    ) {}
}
