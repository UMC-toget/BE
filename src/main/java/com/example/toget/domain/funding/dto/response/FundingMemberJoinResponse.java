package com.example.toget.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record FundingMemberJoinResponse(
        @Schema(description = "펀딩 ID") Long fundingId,
        @Schema(description = "생성된 펀딩 멤버 ID") Long memberId
) {}
