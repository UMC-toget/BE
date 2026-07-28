package com.example.toget.domain.funding.controller;

import com.example.toget.domain.funding.dto.request.FundingReviewCreateRequest;
import com.example.toget.domain.funding.dto.request.FundingReviewTitledCreateRequest;
import com.example.toget.domain.funding.dto.response.FundingReviewCreateResponse;
import com.example.toget.domain.funding.dto.response.FundingReviewDetailResponse;
import com.example.toget.domain.funding.dto.response.FundingReviewInvitationResponse;
import com.example.toget.domain.funding.exception.code.FundingSuccessCode;
import com.example.toget.domain.funding.service.FundingReviewService;
import com.example.toget.domain.user.controller.LoginUserId;
import com.example.toget.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "펀딩 - 개설자/공동관리자용 API", description = "개설자 전용 펀딩 관리 API")
@RestController
@RequestMapping("/api/v1/fundings")
@RequiredArgsConstructor
public class FundingReviewController {

    private final FundingReviewService fundingReviewService;

    @Operation(summary = "[MY_GIFT] 선물 후기 작성",
            description = "개설자가 선물 후기를 작성합니다. 펀딩당 1개만 작성 가능합니다.")
    @PostMapping("/{fundingId}/reviews")
    public ApiResponse<FundingReviewCreateResponse> createReview(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long fundingId,
            @Valid @RequestBody FundingReviewCreateRequest request
    ) {
        FundingReviewCreateResponse result = fundingReviewService.createReview(userId, fundingId, request);
        return ApiResponse.onSuccess(FundingSuccessCode.REVIEW_CREATE_OK, result);
    }

    @Operation(summary = "[TOGETHER_GIFT] 전달 소식 작성",
            description = "개설자가 함께 준비한 멤버들에게 전달 소식을 작성합니다. 펀딩당 1개만 작성 가능합니다.")
    @PostMapping("/{fundingId}/news")
    public ApiResponse<FundingReviewCreateResponse> createNews(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long fundingId,
            @Valid @RequestBody FundingReviewTitledCreateRequest request
    ) {
        FundingReviewCreateResponse result = fundingReviewService.createNews(userId, fundingId, request);
        return ApiResponse.onSuccess(FundingSuccessCode.REVIEW_CREATE_OK, result);
    }

    @Operation(summary = "[TOGETHER_GIFT] 마음 전하기 작성",
            description = "개설자가 선물 받는 사람에게 마음을 전하는 글을 작성합니다. 펀딩당 1개만 작성 가능합니다.")
    @PostMapping("/{fundingId}/heartfelt")
    public ApiResponse<FundingReviewCreateResponse> createHeartfelt(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long fundingId,
            @Valid @RequestBody FundingReviewTitledCreateRequest request
    ) {
        FundingReviewCreateResponse result = fundingReviewService.createHeartfelt(userId, fundingId, request);
        return ApiResponse.onSuccess(FundingSuccessCode.REVIEW_CREATE_OK, result);
    }

    @Operation(summary = "게시물 조회 (후기/소식/마음전하기)",
            description = "특정 타입의 게시물을 조회합니다. 로그인 여부와 무관하게 링크를 아는 누구나 조회할 수 있습니다.")
    @GetMapping("/{fundingId}/reviews/{type}")
    public ApiResponse<FundingReviewDetailResponse> getReview(
            @PathVariable Long fundingId,
            @PathVariable String type
    ) {
        FundingReviewDetailResponse result = fundingReviewService.getReview(fundingId, type);
        return ApiResponse.onSuccess(FundingSuccessCode.REVIEW_GET_OK, result);
    }

    @Operation(summary = "후기용 초대장 조회",
            description = "후기/소식/마음전하기 본문을 보기 전에 먼저 보여주는 초대장 정보만 반환합니다.")
    @GetMapping("/{fundingId}/reviews/{type}/invitation")
    public ApiResponse<FundingReviewInvitationResponse> getInvitation(
            @PathVariable Long fundingId,
            @PathVariable String type
    ) {
        FundingReviewInvitationResponse result = fundingReviewService.getInvitation(fundingId, type);
        return ApiResponse.onSuccess(FundingSuccessCode.REVIEW_INVITATION_GET_OK, result);
    }
}