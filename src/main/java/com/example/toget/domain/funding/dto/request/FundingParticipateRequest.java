package com.example.toget.domain.funding.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 펀딩 참여(축하 메시지 + 참여 금액) 요청 DTO.
 * MY_GIFT는 비로그인 참여 가능 — senderName이 참여자가 직접 입력한 값(guestName)이 된다.
 */
public record FundingParticipateRequest(

        @Schema(description = "참여자 이름 (익명 참여 시에도 필수 입력, 표시 여부는 isAnonymous로 제어)", example = "홍길동")
        String senderName,

        @Schema(description = "참여 금액", example = "50000")
        @NotNull(message = "참여 금액은 필수입니다.")
        @PositiveOrZero(message = "참여 금액은 0원 이상이어야 합니다.")
        Long amount,

        @Schema(description = "편지 내용 (띄어쓰기 포함 234자 제한)", example = "생일 축하해요!")
        @Size(max = 234, message = "편지 내용은 띄어쓰기 포함 234자까지 작성 가능합니다.")
        String letter,

        @Schema(description = "익명 여부", example = "false")
        @NotNull(message = "익명 여부는 필수입니다.")
        Boolean isAnonymous,

        @Schema(description = "편지 비공개 여부", example = "false")
        @NotNull(message = "비공개 여부는 필수입니다.")
        Boolean isPrivate,

        @Schema(description = "참여 카드 배경 ID", example = "3")
        @NotNull(message = "배경 색상은 필수입니다.")
        Long backgroundId

) {}