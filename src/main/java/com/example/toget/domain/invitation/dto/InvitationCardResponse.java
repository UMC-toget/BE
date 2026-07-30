package com.example.toget.domain.invitation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 초대장 카드 조회·수정 결과 응답.
 *
 * [설계 포인트]
 *  - creatorName은 조회(GET)에서만 채워진다. 화면의 "from. OO" 표기에 쓰이는 값으로,
 *    초대장 테이블에 없어 Funding -> User 조회로 가져온다.
 *    수정(PUT)은 개설자 본인이 호출하는 API라 이름이 필요 없어 null로 내려간다.
 */
public record InvitationCardResponse(
        @Schema(description = "초대장 카드 ID", example = "1")
        Long invitationCardId,

        @Schema(description = "개설자 이름. 조회 시에만 채워지며, 개설자 정보가 없으면 null", example = "희주")
        String creatorName,

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
