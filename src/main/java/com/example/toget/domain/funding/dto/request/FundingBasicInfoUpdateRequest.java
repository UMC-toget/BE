package com.example.toget.domain.funding.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record FundingBasicInfoUpdateRequest(

        @Schema(description = "펀딩 제목", example = "길동이의 생일 펀딩")
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
        String title,

        @Schema(description = "기념일", example = "2026-08-15")
        @NotNull(message = "기념일은 필수입니다.")
        LocalDate anniversaryDate,

        @Schema(description = "시작일", example = "2026-07-05")
        @NotNull(message = "시작일은 필수입니다.")
        LocalDate startDate,

        @Schema(description = "종료일", example = "2026-08-14")
        @NotNull(message = "종료일은 필수입니다.")
        LocalDate endDate,

        @Schema(description = "소개글", example = "생일 축하해 주셔서 감사합니다!")
        String introduction,

        @Schema(description = "대표 이미지 URL", example = "https://image.com/thumb.png")
        String thumbnailImageUrl

) {}