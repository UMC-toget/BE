package com.example.toget.domain.funding.controller;

import com.example.toget.domain.funding.dto.request.*;
import com.example.toget.domain.funding.dto.response.*;
import com.example.toget.domain.funding.enums.ContributionSortType;
import com.example.toget.domain.funding.exception.code.FundingSuccessCode;
import com.example.toget.domain.funding.service.FundingQueryService;
import com.example.toget.domain.funding.service.FundingService;
import com.example.toget.domain.gift.dto.request.FundingGiftCandidateCreateRequest;
import com.example.toget.domain.gift.dto.request.FundingGiftUpsertRequest;
import com.example.toget.domain.gift.dto.response.FundingGiftCandidateCreateResponse;
import com.example.toget.domain.gift.dto.response.FundingGiftResponse;
import com.example.toget.domain.gift.exception.code.FundingGiftSuccessCode;
import com.example.toget.domain.gift.service.FundingGiftService;
import com.example.toget.domain.gift.dto.response.FundingGiftResponse;
import com.example.toget.domain.user.controller.LoginUserId;
import com.example.toget.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "펀딩 - 개설자/공동관리자용 API", description = "개설자/공동관리자 전용 펀딩 관리 API")
@RestController
@Validated
@RequestMapping("/api/v1/fundings")
@RequiredArgsConstructor
public class FundingController {

    private final FundingService fundingService;
    private final FundingQueryService fundingQueryService;
    private final FundingGiftService fundingGiftService;

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
            @Parameter(hidden = true) @LoginUserId Long userId,
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

    @Operation(
            summary = "선물 공개 범위 설정 수정",
            description = """
                외부 참여자 및 방문자에게 펀딩 정보의 어떤 영역을 노출할지 제어합니다. \
                개설자 본인만 호출할 수 있습니다.

                **제약**
                - **MY_GIFT 유형 전용입니다.** 함께 선물 준비하기(TOGETHER_GIFT)는 공개 설정 자체를 \
                  사용하지 않으므로 요청 시 400 에러가 반환됩니다.
                - 전체 교체(PUT) 방식이라 5개 필드를 모두 보내야 합니다. 일부만 보내면 400 에러입니다.
                - 종료(`ENDED`)된 펀딩도 수정할 수 있습니다. 기본정보 수정과 달리 정산 정합성에 \
                  영향을 주지 않기 때문입니다.

                ⚠️ 현재 이 설정을 실제로 반영하는 외부 방문자용 조회 API는 아직 구현되지 않았습니다. \
                설정값 저장과 개설자 대시보드 조회까지만 동작합니다.
                """
    )
    @PutMapping("/{fundingId}/visibility-settings")
    public ApiResponse<FundingVisibilityUpdateResponse> updateVisibility(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @Parameter(description = "펀딩 ID", example = "1") @PathVariable Long fundingId,
            @Valid @RequestBody FundingVisibilityUpdateRequest request
    ) {
        FundingVisibilityUpdateResponse result = fundingService.updateVisibility(userId, fundingId, request);
        return ApiResponse.onSuccess(FundingSuccessCode.FUNDING_VISIBILITY_UPDATE_OK, result);
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
            summary = "[TOGETHER_GIFT] 참여자 관리 탭 조회",
            description = """
                함께 선물하기 펀딩이 선물을 고르는 중(`SELECTING`)일 때, 전체 참여자 목록을 \
                역할별로(개설자·관리자 / 일반참여자) 구분해 반환합니다.
                
                펀딩 상태가 `SETTLING` 이상으로 전환되면 이 API는 더 이상 사용하지 않습니다.
                
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
            summary = "[TOGETHER_GIFT] 멤버 역할 변경",
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
            summary = "[TOGETHER_GIFT] 정산 내역 탭 조회",
            description = """
                최종 선물이 확정되어 정산이 진행 중(`SETTLING` 이상)일 때, 정산 대상으로 확정된 참여자 목록과 \
                각자의 입금 상태를 반환합니다.
                
                정산 인원에서 제외된 참여자(투표는 했지만 정산 대상에서 빠진 경우)는 목록에 포함되지 않습니다. \
                이 탭은 펀딩이 `SELECTING` 상태일 때는 조회할 수 없습니다.
                
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
            summary = "[TOGETHER_GIFT] 정산 입금 상태 수정",
            description = """
                개설자가 정산 대상자의 입금 상태를 직접 조정합니다.
                
                **허용되는 전이**
                - `PAID` → `CONFIRMED`: 실제 입금을 확인 처리합니다.
                - `CONFIRMED` → `PAID`: 잘못 확인한 경우 되돌립니다.
                - `PAID` → `UNPAID`: 참여자가 실수로 입금 완료를 눌렀거나, 신고를 취소하고 싶을 때 \
                  미입금 상태로 되돌립니다.
                
                **허용되지 않는 것**
                - `CONFIRMED`에서 바로 `UNPAID`로 변경하는 것은 현재 지원하지 않습니다. \
                  먼저 `PAID`로 되돌린 뒤 `UNPAID`로 변경해주세요. (추후 정책 확정 시 직접 전환 지원 예정)
                - 참여자 본인이 입금을 완료했다고 알리는 절차(`UNPAID` → `PAID`)는 이 API가 아니라 \
                  별도의 참여자용 API를 사용합니다.
                - 정산 대상자가 아닌 멤버(정산 확정에서 제외된 멤버)에게는 사용할 수 없습니다.
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

    @Operation(
            summary = "[MY_GIFT] 참여자 목록 탭 조회",
            description = """
                MY_GIFT 펀딩의 후원 기록을 페이지네이션으로 조회합니다.
                
                `sort=LATEST`(기본값)면 최신 등록순, `sort=OLDEST`면 오래된 순으로 정렬됩니다. \
                `participantCount`, `totalAmount`는 페이지와 무관하게 항상 전체 후원 기록 기준의 요약값입니다.
                """
    )
    @GetMapping("/{fundingId}/dashboards/contributions")
    public ApiResponse<FundingContributionListResponse> getContributions(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @Parameter(description = "펀딩 ID", example = "12") @PathVariable Long fundingId,
            @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "LATEST") ContributionSortType sort,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        FundingContributionListResponse result = fundingService.getContributions(userId, fundingId, sort, page, size);
        return ApiResponse.onSuccess(FundingSuccessCode.FUNDING_CONTRIBUTIONS_GET_OK, result);
    }

    @Operation(
            summary = "[MY_GIFT] 기여 금액 수정",
            description = """
                개설자가 제출된 후원 내역의 금액을 수정합니다. 0원 이상만 허용됩니다.
                """
    )
    @PatchMapping("/{fundingId}/contributions/{contributionId}")
    public ApiResponse<FundingContributionAmountUpdateResponse> updateContributionAmount(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @Parameter(description = "펀딩 ID", example = "12") @PathVariable Long fundingId,
            @Parameter(description = "후원 기록 ID", example = "45") @PathVariable Long contributionId,
            @Valid @RequestBody FundingContributionAmountUpdateRequest request
    ) {
        FundingContributionAmountUpdateResponse result = fundingService.updateContributionAmount(
                userId, fundingId, contributionId, request
        );
        return ApiResponse.onSuccess(FundingSuccessCode.FUNDING_CONTRIBUTION_AMOUNT_UPDATE_OK, result);
    }

    @Operation(summary = "[MY_GIFT] 내 선물 페이지 메인 화면 조회",
            description = """
                펀딩 개최자가 본인의 펀딩 제어실에서 모니터링할 정보를 반환합니다.
                외부용 공유 조회와 달리 공개 설정과 무관하게 모금액, 참여자 수, 계좌 정보 등
                원본 상태의 모든 상세 정보를 노출합니다.
                """)
    @GetMapping("/{fundingId}/dashboards/my-gift")
    public ApiResponse<FundingMyGiftDashboardResponse> getMyGiftDashboard(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long fundingId
    ) {
        FundingMyGiftDashboardResponse result = fundingQueryService.getMyGiftDashboard(userId, fundingId);
        return ApiResponse.onSuccess(FundingSuccessCode.MY_GIFT_DASHBOARD_OK, result);
    }

    @Operation(summary = "[TOGETHER_GIFT] 함께 선물하기 메인 화면 조회",
            description = """
                함께 선물하기 펀딩 제어실의 메인 화면 정보를 반환합니다. 응답은 하나의 스키마를 공유하되,
                펀딩 상태(status)에 따라 채워지는 필드가 다릅니다.
                - SELECTING: topGifts(실시간 득표 상위 2개)만 채워짐
                - SETTLING/PURCHASING/DELIVERING: collectedAmount/targetAmount/confirmedGifts가 채워짐
                - ENDED: 위와 동일 + messageIds(최근 축하 메시지 5개 ID)까지 채워짐
                TOGETHER_GIFT 유형 전용입니다.
                """)
    @GetMapping("/{fundingId}/dashboards/together-gift")
    public ApiResponse<FundingTogetherGiftDashboardResponse> getTogetherGiftDashboard(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long fundingId
    ) {
        FundingTogetherGiftDashboardResponse result = fundingQueryService.getTogetherGiftDashboard(userId, fundingId);
        return ApiResponse.onSuccess(FundingSuccessCode.TOGETHER_GIFT_DASHBOARD_OK, result);
    }

    @Operation(summary = "[TOGETHER_GIFT] 선물 후보 등록하기",
            description = "개설자 또는 관리자가 투표 후보로 선물을 등록합니다. 후보(CANDIDATE) 상태로 생성됩니다.")
    @PostMapping("/{fundingId}/gift-candidates")
    public ApiResponse<FundingGiftCandidateCreateResponse> createCandidate(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long fundingId,
            @Valid @RequestBody FundingGiftCandidateCreateRequest request
    ) {
        FundingGiftCandidateCreateResponse result = fundingGiftService.createCandidate(userId, fundingId, request);
        return ApiResponse.onSuccess(FundingGiftSuccessCode.GIFT_CANDIDATE_CREATE_OK, result);
    }

    @Operation(summary = "[MY_GIFT] 수령희망 선물 목록 수정",
            description = "MY_GIFT의 수령 희망 선물 목록을 갱신합니다. fundingGiftId가 있으면 수정, " +
                    "없으면 신규 생성, 요청에서 빠진 기존 항목은 삭제됩니다.")
    @PutMapping("/{fundingId}/gifts")
    public ApiResponse<List<FundingGiftResponse>> updateWishGifts(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long fundingId,
            @Valid @RequestBody List<@Valid FundingGiftUpsertRequest> requests
    ) {
        List<FundingGiftResponse> result = fundingGiftService.updateWishGifts(userId, fundingId, requests);
        return ApiResponse.onSuccess(FundingGiftSuccessCode.GIFT_LIST_UPDATE_OK, result);
    }

    @Operation(summary = "[TOGETHER_GIFT] 선물 및 정산 참여자 확정하기",
            description = """
                후보 선물 중 최종 선물을 확정하고 정산 참여자를 확정합니다.
                총 정산 금액은 확정된 선물 가격 합계로 자동 계산되며, 정산 참여자에게 균등 분배됩니다.
                나머지는 펀딩 참여 등록일이 빠른 순서대로 1원씩 추가 배정됩니다.
                처리 후 펀딩 상태가 SETTLING으로 전환됩니다.
                """)
    @PostMapping("/{fundingId}/confirm-settlement")
    public ApiResponse<FundingConfirmSettlementResponse> confirmSettlement(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long fundingId,
            @Valid @RequestBody FundingConfirmSettlementRequest request
    ) {
        FundingConfirmSettlementResponse result = fundingService.confirmSettlement(userId, fundingId, request);
        return ApiResponse.onSuccess(FundingSuccessCode.CONFIRM_SETTLEMENT_OK, result);
    }
}