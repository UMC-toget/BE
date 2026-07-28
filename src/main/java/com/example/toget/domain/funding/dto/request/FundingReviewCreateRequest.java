package com.example.toget.domain.funding.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
        @Size(max = 15, message = "초대장 제목은 15자를 초과할 수 없습니다.")
        String invitationTitle,

        @Schema(description = "초대장 내용")
        @Size(max = 60, message = "초대장 내용은 60자를 초과할 수 없습니다.")
        String invitationContent,

        @Schema(description = "초대장 캐릭터 ID")
        @NotBlank(message = "초대장 캐릭터 선택은 필수입니다.")
        Long invitationCharacterId,

        @Schema(description = "초대장 배경 ID (invitation_backgrounds 참조)")
        @NotBlank(message = "초대장 배경 선택은 필수입니다.")
        Long invitationBackgroundId
) {}