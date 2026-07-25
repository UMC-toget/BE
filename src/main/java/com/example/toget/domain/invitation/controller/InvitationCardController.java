package com.example.toget.domain.invitation.controller;

import com.example.toget.domain.invitation.dto.InvitationCardResponse;
import com.example.toget.domain.invitation.dto.InvitationCardUpdateRequest;
import com.example.toget.domain.invitation.exception.code.InvitationSuccessCode;
import com.example.toget.domain.invitation.service.InvitationCardService;
import com.example.toget.domain.user.controller.LoginUserId;
import com.example.toget.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 초대장 카드 수정 API 컨트롤러.
 * 펀딩 개최자 본인만 수정할 수 있으며, 로그인 필수.
 */
@Tag(name = "초대장 API", description = "초대장 및 캐릭터 관련 API")
@RestController
@RequestMapping("/api/v1/fundings/{fundingId}/invitations")
@RequiredArgsConstructor
public class InvitationCardController {

    private final InvitationCardService invitationCardService;

    // 초대장 카드 수정 — 대표 캐릭터, 색상 테마, 제목, 본문 갱신 (개최자 전용)
    @Operation(summary = "초대장 카드 수정",
            description = "특정 펀딩의 초대장 카드 정보(제목, 본문, 색상 테마, 대표 캐릭터)를 수정합니다. 펀딩 개최자 본인만 수정할 수 있습니다.")
    @PutMapping
    public ApiResponse<InvitationCardResponse> update(
            @LoginUserId Long userId,
            @PathVariable Long fundingId,
            @Valid @RequestBody InvitationCardUpdateRequest request
    ) {
        return ApiResponse.onSuccess(
                InvitationSuccessCode.INVITATION_UPDATE_OK,
                invitationCardService.update(userId, fundingId, request)
        );
    }
}
