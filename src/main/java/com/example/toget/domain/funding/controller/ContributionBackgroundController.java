package com.example.toget.domain.funding.controller;


import com.example.toget.domain.funding.dto.request.ContributionBackgroundRequest;
import com.example.toget.domain.funding.dto.response.ContributionBackgroundCreateResponse;
import com.example.toget.domain.funding.dto.response.ContributionBackgroundResponse;
import com.example.toget.domain.funding.exception.code.ContributionSuccessCode;
import com.example.toget.domain.funding.service.ContributionBackgroundService;
import com.example.toget.domain.user.controller.LoginUserId;
import com.example.toget.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "펀딩 API", description = "선물 준비/펀딩 및 참여 관련 API")
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

    /**
     * 배경 색상 생성.
     *
     * @deprecated 관리자 권한 체계가 아직 없어 로그인 여부만 확인한다.
     * 추후 고도화에서 User 파트에서 role 체계 도입 예정 — 도입 전까지 프론트 미노출.
     */
    @Deprecated
    @Operation(summary = "[관리자 전용 예정] 배경 색상 생성",
            description = "⚠️ 관리자 권한 체계 도입 전까지 임시로 로그인 사용자 전체에게 열려 있습니다. 프론트 연동 금지.")
    @PostMapping
    public ApiResponse<ContributionBackgroundCreateResponse> create(
            @LoginUserId Long userId,
            @Valid @RequestBody ContributionBackgroundRequest request
    ) {
        ContributionBackgroundCreateResponse result = contributionBackgroundService.create(request);
        return ApiResponse.onSuccess(ContributionSuccessCode.BACKGROUND_CREATE_OK, result);
    }



    /** 배경 색상 수정 (관리자용) */
    /**
     * 배경 색상 수정.
     *
     * @deprecated 관리자 권한 체계가 아직 없어 로그인 여부만 확인한다.
     * User 파트에서 role 체계 도입 예정(이슈 트래킹 중) — 도입 전까지 프론트 미노출.
     */
    @Deprecated
    @Operation(summary = "[관리자 전용 예정] 배경 색상 수정",
            description = "⚠️ 관리자 권한 체계 도입 전까지 임시로 로그인 사용자 전체에게 열려 있습니다. 프론트 연동 금지.")
    @PutMapping("/{id}")
    public ApiResponse<ContributionBackgroundResponse> update(
            @LoginUserId Long userId,
            @PathVariable Long id,
            @Valid @RequestBody ContributionBackgroundRequest request
    ) {
        ContributionBackgroundResponse result = contributionBackgroundService.update(id, request);
        return ApiResponse.onSuccess(ContributionSuccessCode.BACKGROUND_UPDATE_OK, result);
    }


    /**
     * 배경 색상 삭제.
     *
     * @deprecated 관리자 권한 체계가 아직 없어 로그인 여부만 확인한다.
     * User 파트에서 role 체계 도입 예정(이슈 트래킹 중) — 도입 전까지 프론트 미노출.
     */
    @Deprecated
    @Operation(summary = "[관리자 전용 예정] 배경 색상 삭제",
            description = "⚠️ 관리자 권한 체계 도입 전까지 임시로 로그인 사용자 전체에게 열려 있습니다. 프론트 연동 금지.")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @LoginUserId Long userId,
            @PathVariable Long id
    ) {
        contributionBackgroundService.delete(id);
        return ApiResponse.onSuccess(ContributionSuccessCode.BACKGROUND_DELETE_OK, null);
    }
}