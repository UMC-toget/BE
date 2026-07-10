package com.example.toget.domain.invitation.dto;

/** 캐릭터 단건 응답 — 전체 조회(목록)와 수정 결과 응답에 공용으로 사용 */
public record CharacterResponse(
        Long id,
        String name,
        String imageUrl
) {
}
