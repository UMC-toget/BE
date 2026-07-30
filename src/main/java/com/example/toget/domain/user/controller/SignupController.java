package com.example.toget.domain.user.controller;

import com.example.toget.domain.user.dto.SignupCompleteRequest;
import com.example.toget.domain.user.dto.SignupCompleteResponse;
import com.example.toget.domain.user.service.SignupService;
import com.example.toget.global.apiPayload.ApiResponse;
import com.example.toget.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원가입 완료 API 컨트롤러.
 *
 * <p>{@code POST /api/v1/users} — "회원(users) 컬렉션에 새 리소스를 만든다"는 의미라
 * RESTful 규칙(동사 금지, 복수형 명사)에 맞는다. UserController가 /users/me에 매핑되어 있어
 * 컬렉션 경로를 다루는 컨트롤러를 따로 두었다.
 *
 * <p>아직 회원이 아닌 사람이 호출하므로 SecurityConfig에서 permitAll이며,
 * 신원 확인은 요청 본문의 서명된 가입 토큰으로 한다. (issue #61)
 */
@Tag(name = "유저 API", description = "유저 관련 API (인증, 사용자 정보 및 계좌 관리)")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class SignupController {

    private final SignupService signupService;

    @Operation(summary = "회원가입 완료 (프로필 설정)",
            description = "소셜 로그인 응답으로 받은 가입 토큰과 닉네임·프로필 이미지를 함께 보내 회원가입을 완료합니다. "
                    + "이 시점에 회원이 생성되고 Access/Refresh Token이 발급됩니다. "
                    + "이미 가입된 소셜 계정이면 409(USER409_1)를 반환합니다.")
    @PostMapping
    public ApiResponse<SignupCompleteResponse> completeSignup(@Valid @RequestBody SignupCompleteRequest request) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, signupService.completeSignup(request));
    }
}
