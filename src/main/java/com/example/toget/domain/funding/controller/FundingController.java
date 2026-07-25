package com.example.toget.domain.funding.controller;

import com.example.toget.domain.funding.dto.request.*;
import com.example.toget.domain.funding.dto.response.*;
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
    public ApiResponse<Void> endEarly(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @Parameter(description = "펀딩 ID", example = "1") @PathVariable Long fundingId,
            @Valid @RequestBody FundingStatusUpdateRequest request

    ) {
        fundingService.updateStatus(userId, fundingId, request.status());
        return ApiResponse.onSuccess(FundingSuccessCode.FUNDING_STATUS_UPDATE_OK, null);
    }

    @Operation(summary = "선물 준비 페이지 기본정보 수정",
            description = "개설자가 펀딩의 제목, 기념일, 시작일/종료일, 소개글, 대표 이미지를 수정합니다.")
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

    @Operation(
            summary = "참여자 관리 탭 조회",
            description = """
                함께 선물하기 펀딩이 선물을 고르는 중(`SELECTING`)일 때, 전체 참여자 목록을 \
                역할별로(개설자·관리자 / 일반참여자) 구분해 반환합니다.
                
                펀딩 상태가 `SETTLING` 이상으로 전환되면 이 탭은 정산 내역 탭(`GET .../dashboards/settlements`)으로 \
                대체되며, 이 API는 더 이상 사용하지 않습니다.
                
                개설자 본인만 조회할 수 있습니다.
                """
    )
    @GetMapping("/{fundingId}/dashboards/members")
    public ApiResponse<FundingMemberManagementResponse> getMembers(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long fundingId
    ) {
        FundingMemberManagementResponse result = fundingService.getMembers(userId, fundingId);
        return ApiResponse.onSuccess(FundingSuccessCode.FUNDING_MEMBERS_GET_OK, result);
    }

    @Operation(
            summary = "멤버 역할 변경",
            description = """
                개설자가 특정 참여자를 관리자(`ADMIN`)로 위임하거나, 관리자를 일반 참여자(`PARTICIPANT`)로 \
                강등합니다.
                
                개설자(`CREATOR`) 본인의 역할은 이 API로 변경할 수 없습니다.
                """
    )
    @PatchMapping("/{fundingId}/members/{memberId}/role")
    public ApiResponse<Void> updateMemberRole(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long fundingId,
            @PathVariable Long memberId,
            @Valid @RequestBody FundingMemberRoleUpdateRequest request
    ) {
        fundingService.updateMemberRole(userId, fundingId, memberId, request.role());
        return ApiResponse.onSuccess(FundingSuccessCode.FUNDING_MEMBER_ROLE_UPDATE_OK, null);
    }

    @Operation(
            summary = "정산 내역 탭 조회",
            description = """
                최종 선물이 확정되어 정산이 진행 중(`SETTLING` 이상)일 때, 정산 대상으로 확정된 참여자 목록과 \
                각자의 입금 상태를 반환합니다.
                
                정산 인원에서 제외된 참여자(투표는 했지만 정산 대상에서 빠진 경우)는 목록에 포함되지 않습니다. \
                이 탭은 펀딩이 `SELECTING` 상태일 때는 조회할 수 없으며, 그 경우 참여자 관리 탭 \
                (`GET .../dashboards/members`)을 사용해야 합니다.
                
                개설자 본인만 조회할 수 있습니다.
                """
    )
    @GetMapping("/{fundingId}/dashboards/settlements")
    public ApiResponse<FundingSettlementListResponse> getSettlements(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long fundingId
    ) {
        FundingSettlementListResponse result = fundingService.getSettlements(userId, fundingId);
        return ApiResponse.onSuccess(FundingSuccessCode.FUNDING_SETTLEMENTS_GET_OK, result);
    }


    @Operation(
            summary = "정산 입금 상태 수정",
            description = """
                개설자가 정산 대상자의 입금 상태를 확인 처리(`PAID` → `CONFIRMED`)하거나, \
                잘못 확인한 경우 되돌립니다(`CONFIRMED` → `PAID`).
                
                - `UNPAID`로의 변경은 이 API로 처리하지 않습니다.
                - 참여자 본인이 입금을 완료했다고 알리는 절차(`UNPAID` → `PAID`)는 이 API가 아니라 \
                  별도의 참여자용 API를 사용합니다(개설자가 대신 처리할 수 없습니다).
                - 정산 대상자가 아닌 멤버(정산 확정에서 제외된 멤버)에게는 이 API를 사용할 수 없습니다.
                """
    )
    @PatchMapping("/{fundingId}/members/{memberId}/settlement-status")
    public ApiResponse<Void> updateSettlementStatus(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long fundingId,
            @PathVariable Long memberId,
            @Valid @RequestBody FundingSettlementStatusUpdateRequest request
    ) {
        fundingService.updateSettlementStatus(userId, fundingId, memberId, request.settlementStatus());
        return ApiResponse.onSuccess(FundingSuccessCode.FUNDING_SETTLEMENT_STATUS_UPDATE_OK, null);
    }
}