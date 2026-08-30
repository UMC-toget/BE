package com.example.toget.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ContributionResponse(
        @Schema(description = "참여 ID", example = "1")
        Long id,

        @Schema(description = "참여자 이름 (익명이면 null)", example = "홍길동")
        String senderName,

        @Schema(description = "참여 금액", example = "50000")
        Long amount,

        @Schema(description = "편지 내용", example = "생일 축하해요!")
        String content,

        @Schema(description = "익명 여부", example = "false")
        Boolean isAnonymous,

        @Schema(description = "편지 비공개 여부 (true면 content를 비공개 처리)", example = "false")
        Boolean isPrivate,

        @Schema(description = "참여 카드 배경 ID", example = "3")
        Long backgroundId
) {}