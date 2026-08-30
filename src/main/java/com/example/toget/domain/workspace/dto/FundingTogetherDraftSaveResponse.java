package com.example.toget.domain.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record FundingTogetherDraftSaveResponse(
        @Schema(description = "임시 저장된 함께하는 펀딩(Toget)의 ID", example = "10")
        Long togetherDraftsGiftId
) {}
