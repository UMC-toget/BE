package com.example.toget.domain.workspace.controller;

import com.example.toget.domain.user.controller.LoginUserId;
import com.example.toget.domain.workspace.dto.FundingTogetherDraftDetailResponse;
import com.example.toget.domain.workspace.dto.FundingTogetherDraftSaveRequest;
import com.example.toget.domain.workspace.dto.FundingTogetherDraftSaveResponse;
import com.example.toget.domain.workspace.exception.code.WorkspaceSuccessCode;
import com.example.toget.domain.workspace.service.FundingTogetherDraftService;
import com.example.toget.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 함께 선물 준비 임시 작성 API 컨트롤러.
 */
@RestController
@RequestMapping("/api/v1/together-drafts")
@RequiredArgsConstructor
public class FundingTogetherDraftController {

    private final FundingTogetherDraftService fundingTogetherDraftService;

    /** 함께 선물 준비 임시 저장 상세 조회 */
    @GetMapping
    public ApiResponse<FundingTogetherDraftDetailResponse> getDetail(@LoginUserId Long userId) {
        FundingTogetherDraftDetailResponse response = fundingTogetherDraftService.getDetail(userId);
        return ApiResponse.onSuccess(WorkspaceSuccessCode.DRAFT_DETAIL_OK, response);
    }

    /** 함께 선물 준비 임시 저장 */
    @PostMapping
    public ApiResponse<FundingTogetherDraftSaveResponse> save(
            @LoginUserId Long userId,
            @RequestBody FundingTogetherDraftSaveRequest request
      ) {
          FundingTogetherDraftSaveResponse response = fundingTogetherDraftService.save(userId, request);
          return ApiResponse.onSuccess(WorkspaceSuccessCode.DRAFT_SAVE_OK, response);
      }

    /**
     * 함께 선물 준비 임시 저장 삭제
     * JWT 토큰에서 로그인한 회원 ID를 파싱하므로 URL 상에 ID를 노출하지 않음
     */
    @DeleteMapping
    public ApiResponse<Void> delete(@LoginUserId Long userId) {
        fundingTogetherDraftService.delete(userId);
        return ApiResponse.onSuccess(WorkspaceSuccessCode.DRAFT_DELETE_OK, null);
    }
}
