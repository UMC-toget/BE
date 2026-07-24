package com.example.toget.domain.funding.dto.request;

import com.example.toget.domain.funding.enums.FundingType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 펀딩 생성 요청 DTO. fundingType에 따라 필수 필드가 달라진다.
 * - MY_GIFT: userAccountId 필수
 * - TOGETHER_GIFT: userAccountId 선택 (나중에 등록 가능)
 */
public record FundingCreateRequest(

        @Schema(description = "펀딩 유형", example = "MY_GIFT")
        @NotNull(message = "펀딩 유형은 필수입니다.")
        FundingType fundingType,

        @Schema(description = "정산 계좌 ID. MY_GIFT는 필수, TOGETHER_GIFT는 선택", example = "5")
        Long userAccountId,

        @Schema(description = "펀딩 제목", example = "OO의 생일 선물")
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
        String title,

        @Schema(description = "선물 받을 사람 이름", example = "홍길동")
        @NotBlank(message = "받는 사람 이름은 필수입니다.")
        @Size(max = 50)
        String recipientName,

        @Schema(description = "기념일", example = "2026-08-15")
        @NotNull(message = "기념일은 필수입니다.")
        LocalDate anniversaryDate,

        @Schema(description = "시작일", example = "2026-07-01")
        @NotNull(message = "시작일은 필수입니다.")
        LocalDate startDate,

        @Schema(description = "종료일", example = "2026-07-31")
        @NotNull(message = "종료일은 필수입니다.")
        LocalDate endDate,

        @Schema(description = "소개글", example = "생일 축하해 주셔서 감사합니다!")
        String introduction,

        @Schema(description = "썸네일 이미지 URL")
        String thumbnailImageUrl,

        @Schema(description = "목표 금액. MY_GIFT는 필수 0원 이상, TOGETHER_GIFT는 미입력 시 0으로 시작", example = "100000")
        @NotNull(message = "목표 금액은 필수입니다.")
        @PositiveOrZero(message = "목표 금액은 0원 이상이어야 합니다.")
        Long targetAmount,

        @Schema(description = "초대장 정보")
        @Valid
        @NotNull(message = "초대장 정보는 필수입니다.")
        InvitationRequest invitation,


        @Schema(description = "공개 설정")
        @Valid
        @NotNull(message = "공개 설정은 필수입니다.")
        VisibilityRequest visibility,

        @Schema(description = "후보 선물 목록. MY_GIFT는 최소 1개 이상 필수, TOGETHER_GIFT는 비어 있어도 됨 (투표 후 등록)")
        @Valid
        List<GiftRequest> gifts

) {

        public record InvitationRequest(
                @Schema(description = "캐릭터 ID", example = "1")
                @NotNull(message = "캐릭터는 필수입니다.")
                Long characterId,

                @Schema(description = "배경 ID", example = "2")
                @NotNull(message = "배경은 필수입니다.")
                Long backgroundId,

                @Schema(description = "초대장 제목 (최대 15자)", example = "초대합니다!")
                @NotBlank(message = "초대장 제목은 필수입니다.")
                @Size(max = 15, message = "초대장 제목은 15자를 초과할 수 없습니다.")
                String title,

                @Schema(description = "초대장 내용 (최대 60자)", example = "맛있는 식사와 함께 축하해 주세요.")
                @NotBlank(message = "초대장 내용은 필수입니다.")
                @Size(max = 60, message = "초대장 내용은 60자를 초과할 수 없습니다.")
                String content
        ) {}

        public record VisibilityRequest(
                @Schema(description = "진행률 공개 여부") Boolean showProgress,
                @Schema(description = "참여자 수 공개 여부") Boolean showParticipantCount,
                @Schema(description = "참여자 이름 공개 여부") Boolean showParticipantNames,
                @Schema(description = "축하 메시지 공개 여부") Boolean showMessages,
                @Schema(description = "모금액 공개 여부") Boolean showAmount
        ) {}

        public record GiftRequest(
                @Schema(description = "선물 이름", example = "플레이스테이션 5")
                @NotBlank(message = "선물 이름은 필수입니다.")
                String giftName,

                @Schema(description = "선물 가격", example = "628000")
                @Positive(message = "선물 가격은 0원보다 커야 합니다.")
                Long giftPrice,

                @Schema(description = "구매처 URL")
                String giftPurchaseUrl,

                @Schema(description = "이미지 URL")
                String giftImageUrl
        ) {}
}