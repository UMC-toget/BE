package com.example.toget.domain.gift.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record FundingGiftCandidateCreateResponse(
        @Schema(description = "생성된 후보 선물 ID") Long fundingGiftId
) {}