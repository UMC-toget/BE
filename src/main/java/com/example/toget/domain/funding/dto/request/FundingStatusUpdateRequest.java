package com.example.toget.domain.funding.dto.request;

import com.example.toget.domain.funding.enums.FundingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 펀딩 진행 상태 전환 요청 DTO.
 * SELECTING → SETTLING → PURCHASING → DELIVERING -> ENDED 순서로 진행하며,
 * ENDED로는 어느 단계에서든 즉시 전환 가능(조기 종료).
 */
public record FundingStatusUpdateRequest(

        @Schema(
                description = "변경할 상태. TOGETHER_GIFT는 PURCHASING/DELIVERING/ENDED만, " +
                        "MY_GIFT는 ENDED만 허용됩니다. SETTLING은 이 API로 설정할 수 없습니다.",
                example = "PURCHASING"
        )
        @NotNull(message = "상태 값은 필수입니다.")
        FundingStatus status

) {}