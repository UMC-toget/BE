package com.example.toget.domain.invitation.dto;

/** 초대장 배경 색상 단건 응답 — 전체 조회(목록)와 수정 결과 응답에 공용으로 사용 */
public record InvitationBackgroundResponse(
        Long id,
        String name,
        String hexCode
) {
}
