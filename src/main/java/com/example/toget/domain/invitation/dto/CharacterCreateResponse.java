package com.example.toget.domain.invitation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 캐릭터 생성 응답 — 새로 발급된 ID만 전달 */
public record CharacterCreateResponse(
        @Schema(description = "생성된 캐릭터 ID", example = "1")
        Long id
) {
}
