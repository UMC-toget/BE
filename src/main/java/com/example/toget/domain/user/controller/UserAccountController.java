package com.example.toget.domain.user.controller;

import com.example.toget.domain.user.dto.UserAccountCreateResponse;
import com.example.toget.domain.user.dto.UserAccountRequest;
import com.example.toget.domain.user.dto.UserAccountResponse;
import com.example.toget.domain.user.dto.UserAccountUpdateRequest;
import com.example.toget.domain.user.service.UserAccountService;
import com.example.toget.global.apiPayload.ApiResponse;
import com.example.toget.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 정산 계좌 CRUD API 컨트롤러.
 * 목록·생성은 /user-accounts, 수정·삭제는 /user-accounts/{id}로 대상 자원을 URL로 지정하는 REST 관례.
 * 소유권 검사(남의 계좌 ID를 넣은 경우 403)는 서비스 계층에서 수행한다.
 */
@Tag(name = "유저 API", description = "유저 관련 API (인증, 사용자 정보 및 계좌 관리)")
@RestController
@RequestMapping("/api/v1/user-accounts")
@RequiredArgsConstructor
public class UserAccountController {

    private final UserAccountService userAccountService;

    /** 등록 계좌 전체 조회 — 항상 "내" 계좌만 (userId가 토큰에서 나오므로) */
    @Operation(summary = "등록 계좌 전체 조회", description = "로그인한 사용자의 등록된 모든 정산 계좌 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<UserAccountResponse>> getMyAccounts(@LoginUserId Long userId) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, userAccountService.getMyAccounts(userId));
    }

    /** 계좌 등록 (생성) */
    @Operation(summary = "계좌 등록 (생성)", description = "새로운 정산 계좌 정보를 등록합니다.")
    @PostMapping
    public ApiResponse<UserAccountCreateResponse> create(@LoginUserId Long userId,
                                                         @Valid @RequestBody UserAccountRequest request) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, userAccountService.create(userId, request));
    }

    /** 계좌 정보 수정 — PATCH: 보낸 필드만 갱신하는 부분 수정 의미론 (이슈 #10 명세) */
    @Operation(summary = "계좌 정보 수정", description = "기존에 등록된 계좌 정보 중 필요한 필드만 수정합니다.")
    @PatchMapping("/{userAccountId}")
    public ApiResponse<UserAccountResponse> update(@LoginUserId Long userId,
                                                   @PathVariable Long userAccountId,
                                                   @Valid @RequestBody UserAccountUpdateRequest request) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, userAccountService.update(userId, userAccountId, request));
    }

    /** 유저 계좌 삭제 */
    @Operation(summary = "유저 계좌 삭제", description = "등록된 특정 계좌 정보를 제거합니다.")
    @DeleteMapping("/{userAccountId}")
    public ApiResponse<Void> delete(@LoginUserId Long userId, @PathVariable Long userAccountId) {
        userAccountService.delete(userId, userAccountId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }
}
