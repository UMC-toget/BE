package com.example.toget.domain.funding.controller;

import com.example.toget.domain.funding.dto.request.FundingCreateRequest;
import com.example.toget.domain.funding.dto.response.FundingCreateResponse;
import com.example.toget.domain.funding.exception.code.FundingSuccessCode;
import com.example.toget.domain.funding.service.FundingService;
import com.example.toget.domain.user.controller.LoginUserId;
import com.example.toget.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fundings")
@RequiredArgsConstructor
public class FundingController {

    private final FundingService fundingService;

    @PostMapping
    public ApiResponse<FundingCreateResponse> create(
            @LoginUserId Long userId,
            @Valid @RequestBody FundingCreateRequest request
    ) {
        FundingCreateResponse result = fundingService.create(userId, request);
        return ApiResponse.onSuccess(FundingSuccessCode.FUNDING_CREATE_OK, result);
    }
}