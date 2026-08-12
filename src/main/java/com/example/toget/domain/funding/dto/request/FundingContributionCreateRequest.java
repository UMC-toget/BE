package com.example.toget.domain.funding.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record FundingContributionCreateRequest(
        // DB 컬럼(guest_name)이 length=50이라 그에 맞춤 — 비인증 공개 API라 도배성 대용량 입력 방어 목적도 겸함
        @Schema(description = "참여자 이름 (익명이어도 필수 입력)", example = "축하하는 친구")
        @Size(max = 50, message = "참여자 이름은 50자 이하여야 합니다.")
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

        // FE 편지 작성 UI의 글자수 제한(234자, 공백 포함)과 동일하게 맞춘다 — 클라이언트 검증 우회 방어 (issue #129)
        @Schema(description = "편지 내용", example = "길동아 진심으로 생일 축하해!")
        @Size(max = 234, message = "편지 내용은 234자 이하여야 합니다.")
        String content,

        @Schema(description = "편지 비공개 여부", example = "false")
        @NotNull(message = "비공개 여부는 필수입니다.")
        Boolean isPrivate
) {}