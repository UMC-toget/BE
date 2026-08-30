package com.example.toget.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record FundingCreateResponse(
        @Schema(description = "생성된 펀딩 ID", example = "1")
        Long id
) {}