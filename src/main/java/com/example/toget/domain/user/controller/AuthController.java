package com.example.toget.domain.user.controller;

import com.example.toget.domain.user.dto.SocialLoginRequest;
import com.example.toget.domain.user.dto.SocialLoginResponse;
import com.example.toget.domain.user.dto.TokenRefreshRequest;
import com.example.toget.domain.user.dto.TokenResponse;
import com.example.toget.domain.user.service.AuthService;
import com.example.toget.global.apiPayload.ApiResponse;
import com.example.toget.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 API 컨트롤러.
 * 이 경로(/api/v1/auth/tokens/**)는 SecurityConfig에서 permitAll — 로그인 전이므로 토큰 없이 호출 가능.
 * 컨트롤러는 "HTTP ↔ 서비스" 변환만 담당하고 비즈니스 로직은 전부 AuthService에 있다.
 */
@RestController // @Controller + @ResponseBody: 반환 객체를 뷰가 아닌 JSON으로 직렬화
@RequestMapping("/api/v1/auth") // 클래스 공통 URL 접두사
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 소셜 로그인 및 자동 회원가입.
     * @PathVariable: URL 경로의 {provider} 값("kakao"/"google")을 파라미터로 바인딩
     * @Valid: 요청 본문(@RequestBody로 역직렬화된 DTO)의 @NotBlank 등 검증 실행 — 실패 시 400
     */
    @PostMapping("/tokens/{provider}")
    public ApiResponse<SocialLoginResponse> socialLogin(@PathVariable String provider,
                                                        @Valid @RequestBody SocialLoginRequest request) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, authService.socialLogin(provider, request.identityToken()));
    }

    /** access token 재발급 (Refresh Token Rotation) — refresh token은 헤더가 아닌 본문으로 받는다 */
    @PostMapping("/tokens/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, authService.refresh(request.refreshToken()));
    }
}
