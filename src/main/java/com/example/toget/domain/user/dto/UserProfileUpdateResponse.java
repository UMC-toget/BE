package com.example.toget.domain.user.dto;

/** 프로필 수정 응답 — 수정 결과가 반영된 최종 값을 돌려줘 프론트가 재조회 없이 화면을 갱신할 수 있게 한다 */
public record UserProfileUpdateResponse(
        Long userId,
        String nickname,
        String profileImageUrl
) {
}
