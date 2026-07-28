package com.example.toget.domain.funding.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FundingReviewTitledCreateRequest(
        @Schema(description = "제목")
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 50, message = "제목은 50자를 초과할 수 없습니다.")
        String title,

        @Schema(description = "내용")
        @NotBlank(message = "내용은 필수입니다.")
        String content,

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

        @Schema(description = "초대장 배경 ID")
        @NotBlank(message = "초대장 배경 선택은 필수입니다.")
        Long invitationBackgroundId
) {}