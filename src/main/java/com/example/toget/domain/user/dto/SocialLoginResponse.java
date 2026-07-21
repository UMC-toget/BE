package com.example.toget.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/** 소셜 로그인 성공 응답 — 우리 서비스 전용 토큰 한 쌍 + 기본 프로필 */
public record SocialLoginResponse(
        @Schema(description = "사용자 고유 ID", example = "1")
        Long userId,

        @Schema(description = "사용자 이메일", example = "user@example.com")
        String email,

        @Schema(description = "사용자 이름", example = "홍길동")
        String name,

        @Schema(description = "서비스 Access Token (만료 1시간)", example = "eyJhbGciOiJSUzI1NiIs...")
        String accessToken,

        @Schema(description = "서비스 Refresh Token (만료 14일)", example = "eyJhbGciOiJSUzI1NiIs...")
        String refreshToken,

        // isXxx 형태의 boolean은 Jackson이 is-를 접두사로 해석해 JSON 키가 "newUser"로 바뀔 수 있어 키를 고정
        @Schema(description = "신규 가입자 여부 (true인 경우 신규 가입이므로 온보딩 화면 등으로 유도)", example = "true")
        @JsonProperty("isNewUser") boolean isNewUser // true면 이번 로그인으로 자동 가입됨 → 프론트가 온보딩 표시
) {
}
