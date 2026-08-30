package com.example.toget.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record FundingReviewInvitationResponse(
        @Schema(description = "초대장 제목", example = "생일 파티에 초대합니다!")
        String invitationTitle,

        @Schema(description = "초대장 내용", example = "함께 모여 즐거운 시간을 보내요!")
        String invitationContent,

        @Schema(description = "초대장 대표 캐릭터 ID", example = "1")
        Long invitationCharacterId,

        @Schema(description = "초대장 배경 ID", example = "2")
        Long invitationBackgroundId
) {}
