package com.example.toget.domain.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record IndividualFundingDraftSaveResponse(
        @Schema(description = "임시 저장된 개인 펀딩(My)의 ID", example = "5")
        Long myDraftsGiftId
) {
}
