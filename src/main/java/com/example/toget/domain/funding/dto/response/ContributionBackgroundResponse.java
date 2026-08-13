package com.example.toget.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 배경 색상 목록/단건 조회 응답 DTO */
public record ContributionBackgroundResponse(

        @Schema(description = "배경 색상 ID", example = "1")
        Long id,

        @Schema(description = "배경 색상 이름", example = "파스텔 핑크")
        String name,

        @Schema(description = "배경 색상 HEX 코드", example = "#FFB6C1")
        String hexCode
) {}