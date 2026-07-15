package com.example.toget.domain.workspace.controller;

import com.example.toget.domain.user.controller.LoginUserId;
import com.example.toget.domain.workspace.dto.IndividualFundingDraftDetailResponse;
import com.example.toget.domain.workspace.dto.IndividualFundingDraftSaveRequest;
import com.example.toget.domain.workspace.dto.IndividualFundingDraftSaveResponse;
import com.example.toget.domain.workspace.exception.code.WorkspaceSuccessCode;
import com.example.toget.domain.workspace.service.IndividualFundingDraftService;
import com.example.toget.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    /** 내 선물 준비 임시 저장 */
    @PostMapping
    public ApiResponse<IndividualFundingDraftSaveResponse> save(
            @LoginUserId Long userId,
            @RequestBody IndividualFundingDraftSaveRequest request
    ) {
        IndividualFundingDraftSaveResponse response = individualFundingDraftService.save(userId, request);
        return ApiResponse.onSuccess(WorkspaceSuccessCode.DRAFT_SAVE_OK, response);
    }

    /**
     * 내 선물 준비 임시 저장 삭제
     * JWT 토큰에서 로그인한 회원 ID를 파싱하므로 URL 상에 ID를 노출하지 않음
     */
    @DeleteMapping
    public ApiResponse<Void> delete(@LoginUserId Long userId) {
        individualFundingDraftService.delete(userId);
        return ApiResponse.onSuccess(WorkspaceSuccessCode.DRAFT_DELETE_OK, null);
    }
}
