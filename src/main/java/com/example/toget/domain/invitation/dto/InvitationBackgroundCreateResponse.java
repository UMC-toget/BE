package com.example.toget.domain.invitation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 초대장 배경 색상 생성 응답 — 새로 발급된 ID만 전달 */
public record InvitationBackgroundCreateResponse(
        @Schema(description = "생성된 배경 색상 ID", example = "1")
        Long id
) {
}
