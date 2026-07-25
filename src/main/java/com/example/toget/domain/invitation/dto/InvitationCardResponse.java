package com.example.toget.domain.invitation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 초대장 카드 수정 결과 응답 */
public record InvitationCardResponse(
        @Schema(description = "초대장 카드 ID", example = "1")
        Long invitationCardId,

        @Schema(description = "대표 캐릭터 ID", example = "2")
        Long characterId,

        @Schema(description = "배경 색상(색상 테마) ID", example = "3")
        Long backgroundId,

        @Schema(description = "초대장 제목", example = "수정된 초대장 제목")
        String title,

        @Schema(description = "초대장 본문 내용", example = "수정된 초대장 내용입니다.")
        String content
) {
}
