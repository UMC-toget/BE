package com.example.toget.domain.gift.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FundingGiftCandidateCreateRequest(
        @Schema(description = "선물 이미지 URL")
        String giftImageUrl,

        @Schema(description = "선물 이름", example = "플레이스테이션 5 Slim")
        @NotBlank(message = "선물 이름은 필수입니다.")
        String giftName,

        // @NotBlank는 CharSequence 전용이라 Long에 붙이면 매 요청마다 UnexpectedTypeException으로 500이 난다.
        // null 금지는 @NotNull로, 값 검증(0 초과)은 @Positive로 나눠서 건다.
        @Schema(description = "선물 가격", example = "598000")
        @NotNull(message = "선물 가격은 필수입니다.")
        @Positive(message = "선물 가격은 0원보다 커야 합니다.")
        Long giftPrice,

        @Schema(description = "선물 메모")
        @NotBlank(message = "선물 메모는 필수입니다.")
        String note,

        @Schema(description = "구매처 URL")
        String giftPurchaseUrl
) {}