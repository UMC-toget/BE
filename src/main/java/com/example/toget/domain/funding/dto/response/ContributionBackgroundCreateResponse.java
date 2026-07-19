package com.example.toget.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 배경 색상 생성 응답 DTO — 생성된 리소스의 ID만 반환 */
public record ContributionBackgroundCreateResponse(

        @Schema(description = "생성된 배경 색상 ID", example = "2")
        Long id
) {}