package com.example.toget.domain.funding.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FundingSettlementContributionCreateRequest(
        @Schema(description = "축하 카드 배경 ID", example = "1")
        @NotNull(message = "배경 색상은 필수입니다.")
        Long backgroundId,

        // FE 편지 작성 UI의 글자수 제한(234자, 공백 포함)과 동일하게 맞춘다 — 클라이언트 검증 우회 방어 (issue #129)
        @Schema(description = "편지 내용", example = "길동아 진심으로 축하해! 나도 함께할게")
        @Size(max = 234, message = "편지 내용은 234자 이하여야 합니다.")
        String content,

        @Schema(description = "편지 비공개 여부", example = "false")
        @NotNull(message = "비공개 여부는 필수입니다.")
        Boolean isPrivate
) {}
