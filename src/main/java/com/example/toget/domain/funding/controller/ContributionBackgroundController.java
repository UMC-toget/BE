package com.example.toget.domain.funding.controller;


import com.example.toget.domain.funding.dto.request.ContributionBackgroundRequest;
import com.example.toget.domain.funding.dto.response.ContributionBackgroundCreateResponse;
import com.example.toget.domain.funding.dto.response.ContributionBackgroundResponse;
import com.example.toget.domain.funding.exception.code.ContributionSuccessCode;
import com.example.toget.domain.funding.service.ContributionBackgroundService;
import com.example.toget.global.annotation.AdminOnly;
import com.example.toget.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 참여 카드 배경 색상 CRUD API 컨트롤러.
 * 전체 조회(GET)는 비로그인 허용(SecurityConfig permitAll),
 * 생성/수정/삭제는 관리자 전용(@AdminOnly → AdminOnlyInterceptor).
 *
 *
 */
@Tag(name = "관리자 API", description = "카드 배경/캐릭터 등 꾸미기 리소스 관련 API")
@RestController
@RequestMapping("/api/v1/contribution-backgrounds")
@RequiredArgsConstructor
public class ContributionBackgroundController {

    private final ContributionBackgroundService contributionBackgroundService;

    /** 배경 색상 전체 조회 — 비로그인도 조회 가능 (카드 작성 시 비회원도 접근) */
    @Operation(summary = "배경 색상 전체 조회", description = "비로그인도 조회 가능 (카드 작성 시 비회원도 접근)")
    @GetMapping
    public ApiResponse<List<ContributionBackgroundResponse>> getAll() {
        List<ContributionBackgroundResponse> result = contributionBackgroundService.getAll();
        return ApiResponse.onSuccess(ContributionSuccessCode.BACKGROUND_LIST_OK, result);
    }

    /** 배경 색상 생성 — 관리자 전용 */
    @AdminOnly
    @Operation(summary = "[관리자 전용] 배경 색상 생성",
            description = "관리자 계정만 호출할 수 있습니다. 일반 사용자 요청은 403(COMMON403_1)으로 차단됩니다.")
    @PostMapping
    public ApiResponse<ContributionBackgroundCreateResponse> create(
            @Valid @RequestBody ContributionBackgroundRequest request
    ) {
        ContributionBackgroundCreateResponse result = contributionBackgroundService.create(request);
        return ApiResponse.onSuccess(ContributionSuccessCode.BACKGROUND_CREATE_OK, result);
    }

    /** 배경 색상 수정 — 관리자 전용 */
    @AdminOnly
    @Operation(summary = "[관리자 전용] 배경 색상 수정",
            description = "관리자 계정만 호출할 수 있습니다. 일반 사용자 요청은 403(COMMON403_1)으로 차단됩니다.")
    @PutMapping("/{id}")
    public ApiResponse<ContributionBackgroundResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ContributionBackgroundRequest request
    ) {
        ContributionBackgroundResponse result = contributionBackgroundService.update(id, request);
        return ApiResponse.onSuccess(ContributionSuccessCode.BACKGROUND_UPDATE_OK, result);
    }

    /** 배경 색상 삭제 — 관리자 전용 */
    @AdminOnly
    @Operation(summary = "[관리자 전용] 배경 색상 삭제",
            description = "관리자 계정만 호출할 수 있습니다. 일반 사용자 요청은 403(COMMON403_1)으로 차단됩니다.")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable Long id
    ) {
        contributionBackgroundService.delete(id);
        return ApiResponse.onSuccess(ContributionSuccessCode.BACKGROUND_DELETE_OK, null);
    }
}
