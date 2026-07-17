package com.example.toget.domain.funding.controller;


import com.example.toget.domain.funding.dto.request.ContributionBackgroundRequest;
import com.example.toget.domain.funding.dto.response.ContributionBackgroundCreateResponse;
import com.example.toget.domain.funding.dto.response.ContributionBackgroundResponse;
import com.example.toget.domain.funding.exception.code.ContributionSuccessCode;
import com.example.toget.domain.funding.service.ContributionBackgroundService;
import com.example.toget.domain.user.controller.LoginUserId;
import com.example.toget.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contribution-backgrounds")
@RequiredArgsConstructor
public class ContributionBackgroundController {

    private final ContributionBackgroundService contributionBackgroundService;

    /** 배경 색상 전체 조회 — 비로그인도 조회 가능 (카드 작성 시 비회원도 접근) */
    @GetMapping
    public ApiResponse<List<ContributionBackgroundResponse>> getAll() {
        List<ContributionBackgroundResponse> result = contributionBackgroundService.getAll();
        return ApiResponse.onSuccess(ContributionSuccessCode.BACKGROUND_LIST_OK, result);
    }

    /** 배경 색상 생성 (관리자용) */
    @PostMapping
    public ApiResponse<ContributionBackgroundCreateResponse> create(
            @LoginUserId Long userId,
            @Valid @RequestBody ContributionBackgroundRequest request
    ) {
        ContributionBackgroundCreateResponse result = contributionBackgroundService.create(request);
        return ApiResponse.onSuccess(ContributionSuccessCode.BACKGROUND_CREATE_OK, result);
    }

    /** 배경 색상 수정 (관리자용) */
    @PutMapping("/{id}")
    public ApiResponse<ContributionBackgroundResponse> update(
            @LoginUserId Long userId,
            @PathVariable Long id,
            @Valid @RequestBody ContributionBackgroundRequest request
    ) {
        ContributionBackgroundResponse result = contributionBackgroundService.update(id, request);
        return ApiResponse.onSuccess(ContributionSuccessCode.BACKGROUND_UPDATE_OK, result);
    }

    /** 배경 색상 삭제 (관리자용) */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @LoginUserId Long userId,
            @PathVariable Long id
    ) {
        contributionBackgroundService.delete(id);
        return ApiResponse.onSuccess(ContributionSuccessCode.BACKGROUND_DELETE_OK, null);
    }
}