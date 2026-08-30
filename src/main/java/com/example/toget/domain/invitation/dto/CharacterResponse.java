package com.example.toget.domain.invitation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 캐릭터 단건 응답 — 전체 조회(목록)와 수정 결과 응답에 공용으로 사용 */
public record CharacterResponse(
        @Schema(description = "캐릭터 ID", example = "1")
        Long id,

        @Schema(description = "캐릭터 이름", example = "토끼")
        String name,

        @Schema(description = "캐릭터 이미지 URL", example = "https://toget.com/images/rabbit.png")
        String imageUrl
) {
}
