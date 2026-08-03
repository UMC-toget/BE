package com.example.toget.domain.funding.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record FundingSettlementContributionCreateRequest(
        @Schema(description = "축하 카드 배경 ID", example = "1")
        @NotNull(message = "배경 색상은 필수입니다.")
        Long backgroundId,

        @Schema(description = "편지 내용", example = "길동아 진심으로 축하해! 나도 함께할게")
        String content,

        @Schema(description = "편지 비공개 여부", example = "false")
        @NotNull(message = "비공개 여부는 필수입니다.")
        Boolean isPrivate
) {}
