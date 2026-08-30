package com.example.toget.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * POST /api/v1/auth/tokens/{provider} 요청 본문.
 * identityToken: 프론트가 소셜 SDK로 받은 access token (카카오·구글 모두).
 * 구글은 issue #65로 ID token → access token으로 전환됐고, 전환 기간 동안에는 서버가 두 형식을 모두 받는다.
 * @NotBlank: null·빈 문자열·공백만 있는 값 거부 — 컨트롤러의 @Valid가 트리거하며 실패 시 400.
 */
public record SocialLoginRequest(
        @Schema(description = "소셜 SDK로 받은 Access Token (카카오: Access Token, 구글: Access Token — "
                + "전환 기간 동안 구버전 클라이언트의 ID Token도 허용)", example = "ya29.a0AfH6SMB...")
        @NotBlank(message = "identityToken은 필수입니다.")
        String identityToken
) {
}
