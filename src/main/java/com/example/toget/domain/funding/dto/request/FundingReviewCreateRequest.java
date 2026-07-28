package com.example.toget.domain.funding.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record FundingReviewCreateRequest(
        @Schema(description = "후기 내용")
        @NotBlank(message = "내용은 필수입니다.")
        String content,

        @Schema(description = "배경 ID (contribution_backgrounds 참조)")
        @NotNull(message = "배경 색상은 필수입니다.")
        Long backgroundId,

        @Schema(description = "첨부 이미지 URL 목록")
        List<String> images,

        @Schema(description = "초대장 제목")
        String invitationTitle,

        @Schema(description = "초대장 내용")
        String invitationContent,

        @Schema(description = "초대장 캐릭터 ID")
        Long invitationCharacterId,

        @Schema(description = "초대장 배경 ID (invitation_backgrounds 참조)")
        Long invitationBackgroundId
) {}