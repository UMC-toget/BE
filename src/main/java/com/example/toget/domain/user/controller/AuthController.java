package com.example.toget.domain.user.controller;

import com.example.toget.domain.user.dto.SocialLoginRequest;
import com.example.toget.domain.user.dto.SocialLoginResponse;
import com.example.toget.domain.user.dto.TokenRefreshRequest;
import com.example.toget.domain.user.dto.TokenResponse;
import com.example.toget.domain.user.service.AuthService;
import com.example.toget.global.apiPayload.ApiResponse;
import com.example.toget.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 API 컨트롤러.
 * 이 경로(/api/v1/auth/tokens/**)는 SecurityConfig에서 permitAll — 로그인 전이므로 토큰 없이 호출 가능.
 * 컨트롤러는 "HTTP ↔ 서비스" 변환만 담당하고 비즈니스 로직은 전부 AuthService에 있다.
 */
@Tag(name = "유저 API", description = "유저 관련 API (인증, 사용자 정보 및 계좌 관리)")
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
    @Operation(summary = "소셜 로그인 및 회원가입", description = "소셜 제공자(kakao, google)의 identityToken을 사용해 로그인 및 회원가입을 수행합니다.")
    @PostMapping("/tokens/{provider}")
    public ApiResponse<SocialLoginResponse> socialLogin(@PathVariable String provider,
                                                        @Valid @RequestBody SocialLoginRequest request) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, authService.socialLogin(provider, request.identityToken()));
    }

    /** access token 재발급 (Refresh Token Rotation) — refresh token은 헤더가 아닌 본문으로 받는다 */
    @Operation(summary = "토큰 재발급", description = "만료된 Access Token을 Refresh Token을 사용해 재발급합니다.")
    @PostMapping("/tokens/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, authService.refresh(request.refreshToken()));
    }

    /**
     * 로그아웃 — 서버에 저장된 refresh 토큰을 무효화한다.
     * 이 경로는 /tokens/** 가 아니라 permitAll 대상이 아니므로, SecurityConfig의 anyRequest().authenticated()에
     * 의해 로그인(유효한 JWT)이 필요하다. 사용자 식별은 URL/본문이 아닌 @LoginUserId(토큰)로만 한다.
     */
    @Operation(summary = "로그아웃", description = "로그인한 사용자의 Refresh Token을 무효화하여 세션을 종료합니다.")
    @DeleteMapping("/tokens/me")
    public ApiResponse<Void> logout(@LoginUserId Long userId) {
        authService.logout(userId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }
}
