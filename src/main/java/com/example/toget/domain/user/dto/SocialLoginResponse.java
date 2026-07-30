package com.example.toget.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 소셜 로그인 응답 — isProfileCompleted 값에 따라 채워지는 필드가 달라진다.
 *
 * <p><b>true (기존 회원)</b>: userId·accessToken·refreshToken이 채워지고 signupToken은 null.
 * 곧바로 서비스 이용이 가능하다.
 *
 * <p><b>false (미가입자)</b>: signupToken만 채워지고 나머지는 null.
 * 아직 회원이 아니므로 userId도 없고 서비스 API를 호출할 수도 없다.
 * 프론트는 프로필 설정 화면으로 이동해 {@code POST /api/v1/users}에 signupToken을 함께 보내야 하며,
 * 진짜 accessToken/refreshToken은 그 응답에서 받는다. (issue #61)
 */
public record SocialLoginResponse(
        @Schema(description = "사용자 고유 ID (미가입자는 null)", example = "1")
        Long userId,

        @Schema(description = "사용자 이메일", example = "user@example.com")
        String email,

        @Schema(description = "사용자 이름 (소셜 프로필 기준)", example = "홍길동")
        String name,

        @Schema(description = "서비스 Access Token (만료 1시간). 미가입자는 null", example = "eyJhbGciOiJIUzI1NiIs...")
        String accessToken,

        @Schema(description = "서비스 Refresh Token (만료 14일). 미가입자는 null", example = "eyJhbGciOiJIUzI1NiIs...")
        String refreshToken,

        @Schema(description = "가입 토큰 (만료 10분). 미가입자에게만 발급되며 POST /api/v1/users 호출 시 그대로 전달해야 한다. "
                + "기존 회원은 null", example = "eyJhbGciOiJIUzI1NiIs...")
        String signupToken,

        // isXxx 형태의 boolean은 Jackson이 is-를 접두사로 해석해 JSON 키가 "profileCompleted"로 바뀔 수 있어 키를 고정
        @Schema(description = "가입 완료 여부. false면 아직 회원이 아니므로 프로필 설정 화면으로 유도해야 한다.",
                example = "false")
        @JsonProperty("isProfileCompleted") boolean isProfileCompleted
) {
}
