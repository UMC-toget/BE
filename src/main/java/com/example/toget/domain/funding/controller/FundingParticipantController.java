package com.example.toget.domain.funding.controller;


import com.example.toget.domain.funding.dto.request.FundingContributionCreateRequest;
import com.example.toget.domain.funding.dto.response.FundingContributionCreateResponse;
import com.example.toget.domain.funding.dto.response.FundingContributionDetailResponse;
import com.example.toget.domain.funding.dto.response.FundingContributionListResponse;
import com.example.toget.domain.funding.dto.response.FundingContributionRollingPaperResponse;
import com.example.toget.domain.funding.exception.code.FundingSuccessCode;
import com.example.toget.domain.funding.service.FundingContributionService;
import com.example.toget.domain.user.controller.LoginUserId;
import com.example.toget.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * TODO: 현재 모든 엔드포인트가 @LoginUserId로 로그인을 강제한다.
 * 원래 명세상 비회원도 참여/조회 가능해야 하나,
 * LoginUserIdArgumentResolver가 토큰 없으면 무조건 401을 던지는 구조라
 * user 담당자와 함께 리졸버에 required=false 옵션을 추가하는 작업이 선행되어야 한다.
 * 그 전까지는 로그인 필수로 우선 동작시킨다.
 */
@Tag(name = "펀딩 - 참여자용 API", description = "참여자/방문자용 펀딩 참여 API")
@RestController
@RequestMapping("/api/v1/fundings")
@RequiredArgsConstructor
public class FundingParticipantController {

    private final FundingContributionService fundingContributionService;

    @Operation(summary = "펀딩 참여(후원) 제출",
            description = "비회원이 펀딩 참여 결제를 하거나 기여를 생성할 때 호출합니다. 마음만 보내는 경우 amount는 0으로 보냅니다.")
    @PostMapping("/{fundingId}/contributions")
    public ApiResponse<FundingContributionCreateResponse> createContribution(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long fundingId,
            @Valid @RequestBody FundingContributionCreateRequest request
    ) {
        FundingContributionCreateResponse result =
                fundingContributionService.createContribution(userId, fundingId, request);
        return ApiResponse.onSuccess(FundingSuccessCode.CONTRIBUTION_CREATE_OK, result);
    }

    @Operation(summary = "축하 메시지 전체 조회",
            description = """
                    펀딩 초대장 상세의 응원 타임라인을 구성하기 위한 참여자 리스트와 축하 롤링페이퍼 데이터를 반환합니다.
                    개설자가 아닌 방문자가 조회하는 경우, isAnonymous=true면 senderName이, isPrivate=true면 content가 null로 내려갑니다.
                    개설자 본인이 조회하면 은닉 없이 원본 그대로 내려갑니다.
                    """)
    @GetMapping("/{fundingId}/contributions")
    public ApiResponse<FundingContributionRollingPaperResponse> getContributions(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long fundingId
    ) {
        FundingContributionRollingPaperResponse result = fundingContributionService.getContributions(fundingId, userId);
        return ApiResponse.onSuccess(FundingSuccessCode.CONTRIBUTION_LIST_OK, result);
    }

    @Operation(summary = "축하 메시지 상세 조회",
            description = "특정 후원 카드의 편지 내용만 조회합니다. isPrivate=true면 content는 null로 내려갑니다.")
    @GetMapping("/{fundingId}/contributions/{contributionId}")
    public ApiResponse<FundingContributionDetailResponse> getContributionDetail(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long fundingId,
            @PathVariable Long contributionId
    ) {
        FundingContributionDetailResponse result =
                fundingContributionService.getContributionDetail(fundingId, contributionId, userId);
        return ApiResponse.onSuccess(FundingSuccessCode.CONTRIBUTION_DETAIL_OK, result);
    }
}
