package com.example.toget.domain.funding.controller;

import com.example.toget.domain.funding.dto.request.FundingAccountUpdateRequest;
import com.example.toget.domain.funding.dto.request.FundingBasicInfoUpdateRequest;
import com.example.toget.domain.funding.dto.request.FundingCreateRequest;
import com.example.toget.domain.funding.dto.request.FundingStatusUpdateRequest;
import com.example.toget.domain.funding.dto.response.FundingAccountResponse;
import com.example.toget.domain.funding.dto.response.FundingAccountUpdateResponse;
import com.example.toget.domain.funding.dto.response.FundingBasicInfoResponse;
import com.example.toget.domain.funding.dto.response.FundingCreateResponse;
import com.example.toget.domain.funding.exception.code.FundingSuccessCode;
import com.example.toget.domain.funding.service.FundingService;
import com.example.toget.domain.user.controller.LoginUserId;
import com.example.toget.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "펀딩 (개설자/공동관리자용) API", description = "개설자/공동관리자 전용 펀딩 관리 API")
@RestController
@RequestMapping("/api/v1/fundings")
@RequiredArgsConstructor
public class FundingController {

    private final FundingService fundingService;

    @Operation(
            summary = "선물 준비 생성 (개최)",
            description = """
                새 선물 준비 페이지를 개설합니다.
                
                - **MY_GIFT**: 내 선물 만들기. `userAccountId`(정산 계좌)와 \
                  `gifts`(최소 1개 이상)가 필수입니다.
                - **TOGETHER_GIFT**: 함께 선물 준비하기. `userAccountId`는 선택이며, \
                  `gifts`는 비워둘 수 있습니다 (개설 후 `PUT /fundings/{id}/gifts`로 \
                  후보 선물을 별도 등록·투표·확정하는 흐름). \
                  개설자가 자동으로 개설자(CREATOR) 권한의 멤버로 등록됩니다.
                """
    )
    @PostMapping
    public ApiResponse<FundingCreateResponse> create(
            @LoginUserId Long userId,
            @Valid @RequestBody FundingCreateRequest request
    ) {
        FundingCreateResponse result = fundingService.create(userId, request);
        return ApiResponse.onSuccess(FundingSuccessCode.FUNDING_CREATE_OK, result);
    }


    @Operation(
            summary = "선물 준비 진행 상태 수정",
            description = """
                본인이 개최한 선물 준비 페이지의 진행 상태를 전환합니다.
                
                **TOGETHER_GIFT**
                - 상태 전이 순서: `SELECTING` → `SETTLING` → `PURCHASING` → `DELIVERING` → `ENDED`
                - `ENDED`로는 위 순서와 무관하게 어느 단계에서든 즉시 전환할 수 있습니다 (조기 종료).
                - ⚠️ **`SELECTING` → `SETTLING` 전환은 이 API로 처리하지 않습니다.** \
                  최종 선물과 정산 참여자를 함께 확정해야 하므로, \
                  `POST /fundings/{fundingId}/confirm-settlement`(선물 확정하기) API를 사용해주세요. \
                  이 API로 `SETTLING`을 요청하면 400 에러가 반환됩니다.
                - 이 API가 실제로 처리하는 전이는 `PURCHASING`, `DELIVERING`, `ENDED` 세 가지입니다.
                
                **MY_GIFT**
                - `SETTLING` → `ENDED` 전환만 가능합니다. 후보 선정·구매·전달 단계가 없어 \
                  다른 상태값은 사용하지 않습니다.
                - `ENDED` 외의 상태를 요청하면 400 에러가 반환됩니다.
                """
    )
    @PatchMapping("/{fundingId}/status")
    public ApiResponse<Void> updateStatus(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @Parameter(description = "펀딩 ID", example = "1") @PathVariable Long fundingId,
            @Valid @RequestBody FundingStatusUpdateRequest request

    ) {
        fundingService.updateStatus(userId, fundingId, request.status());
        return ApiResponse.onSuccess(FundingSuccessCode.FUNDING_STATUS_UPDATE_OK, null);
    }

    @Operation(
            summary = "선물 준비 페이지 기본정보 수정",
            description = """
                개설자가 펀딩의 제목, 기념일, 시작일/종료일, 소개글, 대표 이미지를 수정합니다.
                
                **상태별 제약**
                - 펀딩이 종료(`ENDED`)된 이후에는 어떤 필드도 수정할 수 없습니다.
                - 시작일 또는 종료일(기간)을 변경하는 요청은 `SELECTING`, `SETTLING` 상태에서만 \
                  허용됩니다. `PURCHASING`, `DELIVERING` 단계에서는 제목·소개글 등 기간 외 필드만 \
                  수정 가능하며, 기간 값을 함께 변경하려는 요청은 거부됩니다.
                - 기간을 변경하지 않고 요청 바디에 기존과 동일한 `startDate`/`endDate`를 그대로 \
                  담아 보내면, 어느 상태에서든(ENDED 제외) 정상 처리됩니다.
                """
    )
    @PutMapping("/{fundingId}/basic-info")
    public ApiResponse<FundingBasicInfoResponse> updateBasicInfo(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long fundingId,
            @Valid @RequestBody FundingBasicInfoUpdateRequest request
    ) {
        FundingBasicInfoResponse result = fundingService.updateBasicInfo(userId, fundingId, request);
        return ApiResponse.onSuccess(FundingSuccessCode.FUNDING_BASIC_INFO_UPDATE_OK, result);
    }

    @Operation(summary = "선물 준비 페이지 정산 계좌 조회",
            description = "정산금이 입금될 계좌 정보를 조회합니다.")
    @GetMapping("/{fundingId}/account")
    public ApiResponse<FundingAccountResponse> getAccount(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long fundingId
    ) {
        FundingAccountResponse result = fundingService.getAccount(userId, fundingId);
        return ApiResponse.onSuccess(FundingSuccessCode.FUNDING_ACCOUNT_GET_OK, result);
    }

    @Operation(summary = "선물 준비 페이지 정산 계좌 수정",
            description = "개설자가 정산금을 받을 계좌를 변경합니다. 본인 소유의 계좌만 등록 가능합니다.")
    @PatchMapping("/{fundingId}/account")
    public ApiResponse<FundingAccountUpdateResponse> updateAccount(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long fundingId,
            @Valid @RequestBody FundingAccountUpdateRequest request
    ) {
        FundingAccountUpdateResponse result = fundingService.updateAccount(userId, fundingId, request);
        return ApiResponse.onSuccess(FundingSuccessCode.FUNDING_ACCOUNT_UPDATE_OK, result);
    }
}