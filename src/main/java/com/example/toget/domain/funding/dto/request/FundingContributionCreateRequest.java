package com.example.toget.domain.funding.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record FundingContributionCreateRequest(
        @Schema(description = "참여자 이름 (익명이어도 필수 입력)", example = "축하하는 친구")
        String senderName,

        @Schema(description = "축하 카드 배경 ID", example = "1")
        @NotNull(message = "배경 색상은 필수입니다.")
        Long backgroundId,

        @Schema(description = "익명 여부", example = "false")
        @NotNull(message = "익명 여부는 필수입니다.")
        Boolean isAnonymous,

        @Schema(description = "참여 금액 (마음만 보내는 경우 0)", example = "50000")
        @NotNull(message = "금액은 필수입니다.")
        @PositiveOrZero(message = "금액은 0원 이상이어야 합니다.")
        Long amount,

        @Schema(description = "편지 내용", example = "길동아 진심으로 생일 축하해!")
        String content,

        @Schema(description = "편지 비공개 여부", example = "false")
        @NotNull(message = "비공개 여부는 필수입니다.")
        Boolean isPrivate
) {}