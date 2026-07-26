package com.example.toget.domain.gift.controller;

import com.example.toget.domain.gift.dto.request.FundingGiftCandidateCreateRequest;
import com.example.toget.domain.gift.dto.request.FundingGiftCommentCreateRequest;
import com.example.toget.domain.gift.dto.response.*;
import com.example.toget.domain.gift.exception.code.FundingGiftSuccessCode;
import com.example.toget.domain.gift.service.FundingGiftService;
import com.example.toget.domain.user.controller.LoginUserId;
import com.example.toget.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "펀딩 - 참여자용", description = "참여자/방문자용 펀딩 참여 API")
@RestController
@RequestMapping("/api/v1/fundings")
@RequiredArgsConstructor
public class FundingGiftController {

    private final FundingGiftService fundingGiftService;

    @Operation(summary = "선물 후보 리스트 조회",
            description = "함께 선물하기 펀딩의 후보(CANDIDATE) 상태 선물 목록을 반환합니다. votedGiftIds는 요청자가 투표한 후보 ID입니다.")
    @GetMapping("/{fundingId}/gift-candidates")
    public ApiResponse<FundingGiftCandidateListResponse> getCandidates(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long fundingId
    ) {
        FundingGiftCandidateListResponse result = fundingGiftService.getCandidates(fundingId, userId);
        return ApiResponse.onSuccess(FundingGiftSuccessCode.GIFT_CANDIDATE_LIST_OK, result);
    }

    @Operation(summary = "선물 후보 등록하기",
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

    @Operation(summary = "선물 후보 상세 조회",
            description = "특정 선물 후보의 상세 정보와 댓글 목록을 반환합니다.")
    @GetMapping("/{fundingId}/gift-candidates/{fundingGiftId}")
    public ApiResponse<FundingGiftCandidateDetailResponse> getCandidateDetail(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long fundingId,
            @PathVariable Long fundingGiftId
    ) {
        FundingGiftCandidateDetailResponse result =
                fundingGiftService.getCandidateDetail(fundingId, fundingGiftId, userId);
        return ApiResponse.onSuccess(FundingGiftSuccessCode.GIFT_CANDIDATE_DETAIL_OK, result);
    }

    @Operation(summary = "선물 후보 투표/취소 (토글)",
            description = "이미 투표한 상태면 취소, 아니면 새로 투표합니다. 멤버당 최대 3개까지 가능합니다.")
    @PostMapping("/{fundingId}/gift-candidates/{fundingGiftId}/vote")
    public ApiResponse<FundingGiftVoteToggleResponse> toggleVote(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long fundingId,
            @PathVariable Long fundingGiftId
    ) {
        FundingGiftVoteToggleResponse result = fundingGiftService.toggleVote(userId, fundingId, fundingGiftId);
        return ApiResponse.onSuccess(FundingGiftSuccessCode.GIFT_VOTE_TOGGLE_OK, result);
    }

    @Operation(summary = "선물 후보에 댓글 달기",
            description = "선물 후보에 댓글을 작성합니다. 요청자는 해당 펀딩의 멤버여야 합니다.")
    @PostMapping("/{fundingId}/gift-candidates/{fundingGiftId}/comments")
    public ApiResponse<FundingGiftCommentCreateResponse> createComment(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long fundingId,
            @PathVariable Long fundingGiftId,
            @Valid @RequestBody FundingGiftCommentCreateRequest request
    ) {
        FundingGiftCommentCreateResponse result =
                fundingGiftService.createComment(userId, fundingId, fundingGiftId, request);
        return ApiResponse.onSuccess(FundingGiftSuccessCode.GIFT_COMMENT_CREATE_OK, result);
    }
}