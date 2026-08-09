package com.example.toget.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/users 요청 본문 — 프로필 설정과 동시에 회원가입을 완료한다.
 *
 * <p>[signupToken을 헤더가 아닌 본문으로 받는 이유]
 * Authorization 헤더에 실으면 JwtAuthenticationFilter가 access token으로 해석해 401로 끊는다.
 * (그 필터는 "토큰을 들고 온 요청은 회원으로 판정되거나 401" 이라는 계약을 지키도록 되어 있다)
 * 가입 완료는 아직 회원이 아닌 사람이 호출하는 permitAll 엔드포인트라 본문으로 받는 편이 안전하다.
 * refresh token을 본문으로 받는 기존 {@link TokenRefreshRequest}와도 방식이 일치한다.
 *
 * <p>provider·oauthId를 클라이언트에서 직접 받지 않는 것이 핵심이다 —
 * 그대로 받으면 남의 소셜 계정으로 가입할 수 있다. 서명된 signupToken 안의 값만 신뢰한다.
 */
public record SignupCompleteRequest(
        @Schema(description = "소셜 로그인 응답으로 받은 가입 토큰", example = "eyJhbGciOiJIUzI1NiIs...")
        @NotBlank(message = "signupToken은 필수입니다.")
        String signupToken,

        // 가입 확정의 전제 조건이므로 프로필 수정(PATCH)과 달리 필수다
        @Schema(description = "사용할 닉네임", example = "투겟러브")
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 1, max = 6, message = "닉네임은 1자 이상 6자 이하여야 합니다.")
        String nickname,

        @Schema(description = "프로필 이미지 URL. 미설정 시 null로 보내면 기본 이미지가 사용된다.",
                example = "https://toget.com/images/profile.png")
        // 컬럼이 TEXT로 확장됐지만(issue #102) 무제한 입력을 받을 이유는 없어 상한은 유지한다.
        @Size(max = 2048, message = "프로필 이미지 URL은 2048자 이하여야 합니다.")
        String profileImageUrl
) {
}
