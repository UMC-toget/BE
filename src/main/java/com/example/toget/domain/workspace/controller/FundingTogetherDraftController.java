package com.example.toget.domain.workspace.controller;

import com.example.toget.domain.user.controller.LoginUserId;
import com.example.toget.domain.workspace.dto.FundingTogetherDraftDetailResponse;
import com.example.toget.domain.workspace.dto.FundingTogetherDraftSaveRequest;
import com.example.toget.domain.workspace.dto.FundingTogetherDraftSaveResponse;
import com.example.toget.domain.workspace.exception.code.WorkspaceSuccessCode;
import com.example.toget.domain.workspace.service.FundingTogetherDraftService;
import com.example.toget.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

/**
 * 함께 선물 준비 임시 작성 API 컨트롤러.
 */
@Tag(name = "임시 저장 API", description = "선물 준비 임시저장 API (내 선물 및 함께 선물)")
@RestController
@RequestMapping("/api/v1/together-drafts")
@RequiredArgsConstructor
public class FundingTogetherDraftController {

    private final FundingTogetherDraftService fundingTogetherDraftService;

    /** 함께 선물 준비 임시 저장 상세 조회 */
    @Operation(summary = "함께 선물 준비 임시 저장 상세 조회", description = "작성 중이던 함께 선물 준비의 임시 저장 데이터를 조회합니다.")
    @GetMapping
    public ApiResponse<FundingTogetherDraftDetailResponse> getDetail(@LoginUserId Long userId) {
        FundingTogetherDraftDetailResponse response = fundingTogetherDraftService.getDetail(userId);
        return ApiResponse.onSuccess(WorkspaceSuccessCode.DRAFT_DETAIL_OK, response);
    }

    /** 함께 선물 준비 임시 저장 */
    @Operation(summary = "함께 선물 준비 임시 저장", description = "작성 중인 함께 선물 준비의 진행 상태 및 설정을 임시 저장합니다.")
    @PostMapping
    public ApiResponse<FundingTogetherDraftSaveResponse> save(
            @LoginUserId Long userId,
            @Valid @RequestBody FundingTogetherDraftSaveRequest request
      ) {
          FundingTogetherDraftSaveResponse response = fundingTogetherDraftService.save(userId, request);
          return ApiResponse.onSuccess(WorkspaceSuccessCode.DRAFT_SAVE_OK, response);
      }

    /**
     * 함께 선물 준비 임시 저장 삭제
     * JWT 토큰에서 로그인한 회원 ID를 파싱하므로 URL 상에 ID를 노출하지 않음
     */
    @Operation(summary = "함께 선물 준비 임시 저장 삭제", description = "저장된 함께 선물 준비 임시 저장 데이터를 영구 삭제합니다.")
    @DeleteMapping
    public ApiResponse<Void> delete(@LoginUserId Long userId) {
        fundingTogetherDraftService.delete(userId);
        return ApiResponse.onSuccess(WorkspaceSuccessCode.DRAFT_DELETE_OK, null);
    }
}
