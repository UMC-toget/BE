package com.example.toget.domain.funding.dto.request;

import com.example.toget.domain.funding.enums.FundingType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

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

        @Schema(description = "소개글")
        String introduction,

        @Schema(description = "썸네일 이미지 URL")
        String thumbnailImageUrl,

        @Schema(description = "목표 금액. MY_GIFT는 필수 0원 이상, TOGETHER_GIFT는 미입력 시 0으로 시작", example = "100000")
        @PositiveOrZero(message = "목표 금액은 0원 이상이어야 합니다.")
        Long targetAmount

) {}