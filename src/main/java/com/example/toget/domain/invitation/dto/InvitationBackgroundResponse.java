package com.example.toget.domain.invitation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 초대장 배경 색상 단건 응답 — 전체 조회(목록)와 수정 결과 응답에 공용으로 사용 */
public record InvitationBackgroundResponse(
        @Schema(description = "배경 색상 ID", example = "1")
        Long id,

        @Schema(description = "배경 색상 이름", example = "파스텔 옐로우")
        String name,

        @Schema(description = "배경 색상 HEX 코드", example = "#FFFFE0")
        String hexCode
) {
}
