package com.example.toget.domain.workspace.controller;

import com.example.toget.domain.user.controller.LoginUserId;
import com.example.toget.domain.workspace.dto.IndividualFundingDraftDetailResponse;
import com.example.toget.domain.workspace.dto.IndividualFundingDraftSaveRequest;
import com.example.toget.domain.workspace.dto.IndividualFundingDraftSaveResponse;
import com.example.toget.domain.workspace.exception.code.WorkspaceSuccessCode;
import com.example.toget.domain.workspace.service.IndividualFundingDraftService;
import com.example.toget.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

/**
 * 내 선물 준비 임시 작성 API 컨트롤러.
 */
@Tag(name = "임시 저장 API", description = "선물 준비 임시저장 API (내 선물 및 함께 선물)")
@RestController
@RequestMapping("/api/v1/individual-drafts")
@RequiredArgsConstructor
public class IndividualFundingDraftController {

    private final IndividualFundingDraftService individualFundingDraftService;

    /** 내 선물 준비 임시 저장 상세 조회 */
    @Operation(summary = "내 선물 준비 임시 저장 상세 조회", description = "작성 중이던 내 선물 준비의 임시 저장 데이터를 조회합니다.")
    @GetMapping
    public ApiResponse<IndividualFundingDraftDetailResponse> getDetail(@LoginUserId Long userId) {
        IndividualFundingDraftDetailResponse response = individualFundingDraftService.getDetail(userId);
        return ApiResponse.onSuccess(WorkspaceSuccessCode.DRAFT_DETAIL_OK, response);
    }

    /** 내 선물 준비 임시 저장 */
    @Operation(summary = "내 선물 준비 임시 저장", description = "작성 중인 내 선물 준비의 진행 상태 및 설정을 임시 저장합니다.")
    @PostMapping
    public ApiResponse<IndividualFundingDraftSaveResponse> save(
            @LoginUserId Long userId,
            @Valid @RequestBody IndividualFundingDraftSaveRequest request
    ) {
        IndividualFundingDraftSaveResponse response = individualFundingDraftService.save(userId, request);
        return ApiResponse.onSuccess(WorkspaceSuccessCode.DRAFT_SAVE_OK, response);
    }

    /**
     * 내 선물 준비 임시 저장 삭제
     */
    @Operation(summary = "내 선물 준비 임시 저장 삭제", description = "저장된 내 선물 준비 임시 저장 데이터를 영구 삭제합니다.")
    @DeleteMapping("/{draftId}")
    public ApiResponse<Void> delete(
            @LoginUserId Long userId,
            @PathVariable Long draftId
    ) {
        individualFundingDraftService.delete(userId, draftId);
        return ApiResponse.onSuccess(WorkspaceSuccessCode.DRAFT_DELETE_OK, null);
    }
}
