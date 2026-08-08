package com.example.toget.domain.funding.controller;

import com.example.toget.domain.funding.dto.request.FundingReviewCreateRequest;
import com.example.toget.domain.funding.dto.request.FundingReviewTitledCreateRequest;
import com.example.toget.domain.funding.dto.response.FundingReviewCreateResponse;
import com.example.toget.domain.funding.dto.response.FundingReviewDetailResponse;
import com.example.toget.domain.funding.dto.response.FundingReviewInvitationResponse;
import com.example.toget.domain.funding.exception.code.FundingSuccessCode;
import com.example.toget.domain.funding.service.FundingReviewService;
import com.example.toget.domain.gift.dto.request.FundingGiftPurchaseRequest;
import com.example.toget.domain.gift.dto.response.FundingGiftPurchaseResponse;
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

@Tag(name = "펀딩 - 후기 API", description = "선물 후기, 전달 소식, 마음 전하기 작성 및 조회 API")
@RestController
@RequestMapping("/api/v1/fundings")
@RequiredArgsConstructor
public class FundingReviewController {

    private final FundingReviewService fundingReviewService;
    private final FundingGiftService fundingGiftService;

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
            description = """
                특정 타입의 게시물을 조회합니다. 로그인 여부와 무관하게 링크를 아는 누구나 조회할 수 있습니다.

                응답의 authorName(작성자 표시 이름)은 REVIEW(선물 후기)/NEWS(전달 소식)만 채워지고,
                HEARTFELT(마음전하기)는 null입니다. 개설자의 닉네임을 우선 쓰고, 닉네임 미설정 시 이름으로 대체합니다.
                """)
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

    @Operation(summary = "구매 내역 업로드",
            description = "개설자가 확정된 선물의 실제 구매 내역(구매링크, 영수증 이미지)을 업로드합니다.")
    @PostMapping("/{fundingId}/gifts/{fundingGiftId}/purchase")
    public ApiResponse<FundingGiftPurchaseResponse> uploadPurchase(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long fundingId,
            @PathVariable Long fundingGiftId,
            @Valid @RequestBody FundingGiftPurchaseRequest request
    ) {
        FundingGiftPurchaseResponse result = fundingGiftService.uploadPurchase(userId, fundingId, fundingGiftId, request);
        return ApiResponse.onSuccess(FundingGiftSuccessCode.GIFT_PURCHASE_UPLOAD_OK, result);
    }

}