package com.example.toget.domain.funding.controller;

import com.example.toget.domain.funding.dto.request.FundingCreateRequest;
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

@Tag(name = "펀딩 API", description = "선물 준비/펀딩 및 참여 관련 API")
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
            summary = "선물 준비 조기 종료",
            description = """
                    선물 준비를 즉시 마감합니다. 최종 선물이 확정된 상태든 \
                    확정되지 않은 상태든 종료할 수 있습니다.
                    
                    이미 종료(ENDED)된 페이지를 다시 종료하려고 하면 오류가 발생합니다.
                    """
    )
    @PatchMapping("/{fundingId}/status")
    public ApiResponse<Void> endEarly(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @Parameter(description = "펀딩 ID", example = "1") @PathVariable Long fundingId
    ) {
        fundingService.endEarly(userId, fundingId);
        return ApiResponse.onSuccess(FundingSuccessCode.FUNDING_STATUS_UPDATE_OK, null);
    }

}