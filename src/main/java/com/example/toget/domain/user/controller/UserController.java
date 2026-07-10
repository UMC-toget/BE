package com.example.toget.domain.user.controller;

import com.example.toget.domain.user.dto.UserProfileResponse;
import com.example.toget.domain.user.dto.UserProfileUpdateRequest;
import com.example.toget.domain.user.dto.UserProfileUpdateResponse;
import com.example.toget.domain.user.service.UserService;
import com.example.toget.global.apiPayload.ApiResponse;
import com.example.toget.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 내 프로필 API 컨트롤러.
 * URL이 /users/{id}가 아니라 /users/me인 이유: "누구의" 정보인지를 클라이언트가 아닌
 * 토큰(@LoginUserId)이 결정하게 해서, URL의 ID를 바꿔 남의 정보에 접근하는 것 자체가 불가능하다.
 */
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 내 프로필 정보 조회 — @LoginUserId: JWT에서 추출된 사용자 ID가 리졸버를 통해 주입됨 */
    @GetMapping
    public ApiResponse<UserProfileResponse> getMyProfile(@LoginUserId Long userId) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, userService.getMyProfile(userId));
    }

    /** 내 프로필 정보 수정 — PATCH: 보낸 필드만 갱신하는 부분 수정 의미론 */
    @PatchMapping
    public ApiResponse<UserProfileUpdateResponse> updateMyProfile(@LoginUserId Long userId,
                                                                  @Valid @RequestBody UserProfileUpdateRequest request) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, userService.updateMyProfile(userId, request));
    }

    /** 내 프로필 이미지 삭제 (초기화) */
    @DeleteMapping("/profile-image")
    public ApiResponse<UserProfileUpdateResponse> clearMyProfileImage(@LoginUserId Long userId) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, userService.clearMyProfileImage(userId));
    }

    /** 회원 탈퇴 (Soft Delete) */
    @DeleteMapping
    public ApiResponse<Void> withdraw(@LoginUserId Long userId) {
        userService.withdraw(userId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }

    // GET /fundings (내가 개최한 선물 준비 목록)는 Funding 엔티티(#9 담당 파트) 머지 후 추가 예정
}
