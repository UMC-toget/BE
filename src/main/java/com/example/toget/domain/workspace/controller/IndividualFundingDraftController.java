package com.example.toget.domain.workspace.controller;

import com.example.toget.domain.user.controller.LoginUserId;
import com.example.toget.domain.workspace.dto.IndividualFundingDraftDetailResponse;
import com.example.toget.domain.workspace.exception.code.WorkspaceSuccessCode;
import com.example.toget.domain.workspace.service.IndividualFundingDraftService;
import com.example.toget.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내 선물 준비 임시 작성 API 컨트롤러.
 */
@RestController
@RequestMapping("/api/v1/individual-drafts")
@RequiredArgsConstructor
public class IndividualFundingDraftController {

    private final IndividualFundingDraftService individualFundingDraftService;

    /** 내 선물 준비 임시 저장 상세 조회 */
    @GetMapping
    public ApiResponse<IndividualFundingDraftDetailResponse> getDetail(@LoginUserId Long userId) {
        IndividualFundingDraftDetailResponse response = individualFundingDraftService.getDetail(userId);
        return ApiResponse.onSuccess(WorkspaceSuccessCode.DRAFT_DETAIL_OK, response);
    }
}
