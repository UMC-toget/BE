package com.example.toget.domain.funding.dto.response;

import com.example.toget.domain.funding.enums.FundingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 펀딩 진행 상태 전환 응답 DTO.
 */
public record FundingStatusUpdateResponse(

        @Schema(description = "펀딩 ID", example = "15")
        Long fundingId,

        @Schema(description = "전환된 펀딩 상태", example = "SETTLING")
        FundingStatus status

) {}