package com.example.toget.domain.funding.controller;


import com.example.toget.domain.funding.dto.request.FundingContributionCreateRequest;
import com.example.toget.domain.funding.dto.request.FundingSettlementContributionCreateRequest;
import com.example.toget.domain.funding.dto.response.FundingContributionCreateResponse;
import com.example.toget.domain.funding.dto.response.FundingContributionDetailResponse;
import com.example.toget.domain.funding.dto.response.FundingContributionRollingPaperResponse;
import com.example.toget.domain.funding.dto.response.FundingMemberJoinResponse;
import com.example.toget.domain.funding.dto.response.FundingSettlementContributionCreateResponse;
import com.example.toget.domain.funding.exception.code.FundingSuccessCode;
import com.example.toget.domain.funding.service.FundingContributionService;
import com.example.toget.domain.funding.service.FundingService;
import com.example.toget.domain.user.controller.LoginUserId;
import com.example.toget.global.apiPayload.ApiResponse;
import com.example.toget.global.util.AuthenticatedUserUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 참여자/방문자용 펀딩 참여 API.
 *
 * [설계 포인트]
 *  - 초대장 링크로 들어온 비회원도 호출할 수 있어야 하므로 @LoginUserId를 쓰지 않는다.
 *    @LoginUserId는 토큰이 없으면 리졸버가 401을 던져 비회원 요청을 막아버린다.
 *  - 대신 AuthenticatedUserUtils.getCurrentUserIdOrNull()로 "있으면 받고 없으면 null"을 얻는다.
 *    만료·위조 토큰은 JwtAuthenticationFilter가 먼저 401로 끊으므로, 여기서 null이면
 *    "토큰을 아예 안 보낸 진짜 비회원"이다.
 *  - userId는 개설자 본인 여부(isOwner) 판정에만 쓰인다. 비회원은 항상 isOwner=false로 떨어져
 *    익명·비밀편지·공개 설정에 따른 은닉이 그대로 적용된다.
 */
@Tag(name = "펀딩 - 참여자용 API", description = "참여자/방문자용 펀딩 참여 API")
@RestController
@RequestMapping("/api/v1/fundings")
@RequiredArgsConstructor
public class FundingParticipantController {

    private final FundingContributionService fundingContributionService;
    private final FundingService fundingService;

    @Operation(summary = "[TOGETHER_GIFT] 함께 선물하기 참여(합류)",
            description = """
                    초대장 링크로 들어온 회원이 함께 선물하기 펀딩에 참여자(PARTICIPANT)로 합류합니다.
                    로그인이 필수이며(비회원은 참여자로 등록될 수 없습니다), 이미 참여 중인 회원이 \
                    다시 요청하면 409 에러가 반환됩니다.
                    """)
    @PostMapping("/{fundingId}/members")
    public ApiResponse<FundingMemberJoinResponse> join(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long fundingId
    ) {
        FundingMemberJoinResponse result = fundingService.join(userId, fundingId);
        return ApiResponse.onSuccess(FundingSuccessCode.FUNDING_MEMBER_JOIN_OK, result);
    }

    @Operation(summary = "[TOGETHER_GIFT] 함께 선물하기 나가기",
            description = """
                    로그인한 본인의 참여를 취소합니다. 토큰의 userId로 본인 멤버십만 대상으로 합니다.
                    선물 후보 선정 중(`SELECTING`)일 때만 허용됩니다. \
                    개설자(`CREATOR`)는 나갈 수 없습니다.
                    """)
    @DeleteMapping("/{fundingId}/members/me")
    public ApiResponse<Void> leave(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long fundingId
    ) {
        fundingService.leave(userId, fundingId);
        return ApiResponse.onSuccess(FundingSuccessCode.FUNDING_MEMBER_LEAVE_OK, null);
    }

    @Operation(summary = "[TOGETHER_GIFT] 정산 참여자 입금 완료 신고",
            description = """
                    정산 대상으로 확정된(`amountDue`가 세팅된) 로그인 멤버 본인이 입금 완료를 신고합니다. \
                    축하 메시지(`backgroundId`, `content`, `isPrivate`)를 함께 남기며 참여 기록으로 저장됩니다.

                    `amount`는 정산 확정 시점에 이미 `amountDue`로 고정되어 있어 요청에 포함하지 않고, \
                    익명 여부도 받지 않습니다 — 신원이 이미 확인된 멤버라 항상 실명으로 남습니다.

                    처리 후 본인의 정산 입금 상태가 `UNPAID` → `PAID`로 전환됩니다.
                    """)
    @PostMapping("/{fundingId}/members/me/contributions")
    public ApiResponse<FundingSettlementContributionCreateResponse> createSettlementContribution(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long fundingId,
            @Valid @RequestBody FundingSettlementContributionCreateRequest request
    ) {
        FundingSettlementContributionCreateResponse result =
                fundingService.reportSettlementPayment(userId, fundingId, request);
        return ApiResponse.onSuccess(FundingSuccessCode.SETTLEMENT_CONTRIBUTION_CREATE_OK, result);
    }

    @Operation(summary = "[MY_GIFT] 펀딩 참여(후원) 제출",
            description = "비회원이 펀딩 참여 결제를 하거나 기여를 생성할 때 호출합니다. 마음만 보내는 경우 amount는 0으로 보냅니다.")
    @PostMapping("/{fundingId}/contributions")
    public ApiResponse<FundingContributionCreateResponse> createContribution(
            @PathVariable Long fundingId,
            @Valid @RequestBody FundingContributionCreateRequest request
    ) {
        Long userId = AuthenticatedUserUtils.getCurrentUserIdOrNull();
        FundingContributionCreateResponse result =
                fundingContributionService.createContribution(userId, fundingId, request);
        return ApiResponse.onSuccess(FundingSuccessCode.CONTRIBUTION_CREATE_OK, result);
    }

    @Operation(summary = "축하 메시지 전체 조회",
            description = """
                    펀딩 초대장 상세의 응원 타임라인을 구성하기 위한 참여자 리스트와 축하 롤링페이퍼 데이터를 반환합니다.
                    로그인 없이 호출할 수 있습니다.

                    **은닉 규칙** — 아래 둘 중 하나라도 해당하면 가려집니다.
                    - 참여자 개인 선택: `isAnonymous=true`면 `senderName`이, `isPrivate=true`면 `content`가 null
                    - 개설자 공개 설정: `showParticipantNames=false`면 모든 `senderName`이, \
                      `showMessages=false`면 모든 `content`가 null

                    개설자 본인이 조회하면 은닉 없이 원본 그대로 내려갑니다.
                    공개 설정이 없는 펀딩(함께 선물하기 등)은 전체 공개로 처리됩니다.
                    """)
    @GetMapping("/{fundingId}/contributions")
    public ApiResponse<FundingContributionRollingPaperResponse> getContributions(
            @PathVariable Long fundingId
    ) {
        Long userId = AuthenticatedUserUtils.getCurrentUserIdOrNull();
        FundingContributionRollingPaperResponse result = fundingContributionService.getContributions(fundingId, userId);
        return ApiResponse.onSuccess(FundingSuccessCode.CONTRIBUTION_LIST_OK, result);
    }

    @Operation(summary = "축하 메시지 상세 조회",
            description = """
                    특정 후원 카드의 편지 내용만 조회합니다. 로그인 없이 호출할 수 있습니다.

                    전체 조회와 동일한 은닉 규칙이 적용됩니다. `isPrivate=true`이거나 개설자가 \
                    `showMessages=false`로 설정한 경우 `content`는 null로 내려갑니다. \
                    (목록에서 가려진 메시지를 이 API로 우회 조회할 수 없습니다.)
                    """)
    @GetMapping("/{fundingId}/contributions/{contributionId}")
    public ApiResponse<FundingContributionDetailResponse> getContributionDetail(
            @PathVariable Long fundingId,
            @PathVariable Long contributionId
    ) {
        Long userId = AuthenticatedUserUtils.getCurrentUserIdOrNull();
        FundingContributionDetailResponse result =
                fundingContributionService.getContributionDetail(fundingId, contributionId, userId);
        return ApiResponse.onSuccess(FundingSuccessCode.CONTRIBUTION_DETAIL_OK, result);
    }
}
