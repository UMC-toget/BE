package com.example.toget.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 소셜 로그인 성공 응답 — 우리 서비스 전용 토큰 한 쌍 + 기본 프로필 */
public record SocialLoginResponse(
        Long userId,
        String email,
        String name,
        String accessToken,
        String refreshToken,
        // isXxx 형태의 boolean은 Jackson이 is-를 접두사로 해석해 JSON 키가 "newUser"로 바뀔 수 있어 키를 고정
        @JsonProperty("isNewUser") boolean isNewUser // true면 이번 로그인으로 자동 가입됨 → 프론트가 온보딩 표시
) {
}
