package com.example.toget.domain.gift.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FundingGiftUpsertRequest(
        @Schema(description = "기존 선물 ID. 신규 등록 시 null", example = "3")
        Long fundingGiftId,

        @Schema(description = "선물 이름", example = "플레이스테이션 5 Slim")
        @NotBlank(message = "선물 이름은 필수입니다.")
        String giftName,

        @Schema(description = "선물 가격", example = "598000")
        @NotNull(message = "선물 가격은 필수입니다.")
        @Positive(message = "선물 가격은 0원보다 커야 합니다.")
        Long giftPrice,

        @Schema(description = "구매처 URL")
        String giftPurchaseUrl,

        @Schema(description = "이미지 URL")
        String giftImageUrl
) {}