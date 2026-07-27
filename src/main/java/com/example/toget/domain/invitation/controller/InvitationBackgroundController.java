package com.example.toget.domain.invitation.controller;

import com.example.toget.domain.invitation.dto.InvitationBackgroundCreateResponse;
import com.example.toget.domain.invitation.dto.InvitationBackgroundRequest;
import com.example.toget.domain.invitation.dto.InvitationBackgroundResponse;
import com.example.toget.domain.invitation.exception.code.InvitationSuccessCode;
import com.example.toget.domain.invitation.service.InvitationBackgroundService;
import com.example.toget.global.annotation.AdminOnly;
import com.example.toget.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 초대장 배경 색상 CRUD API 컨트롤러.
 * 전체 조회(GET)는 비로그인 허용(SecurityConfig permitAll),
 * 생성/수정/삭제는 관리자 전용(@AdminOnly → AdminOnlyInterceptor).
 *
 * <p>배경 색상은 특정 사용자의 소유물이 아니라 서비스 전체가 공유하는 마스터 데이터이므로,
 * 참여 카드 배경 색상(ContributionBackgroundController)과 동일하게 관리자만 변경할 수 있다.
 */
@Tag(name = "초대장 API", description = "초대장 및 캐릭터 관련 API")
@RestController
@RequestMapping("/api/v1/invitation-backgrounds")
@RequiredArgsConstructor
public class InvitationBackgroundController {

    private final InvitationBackgroundService invitationBackgroundService;

    // 배경 색상 전체 조회 — 초대장 꾸미기 화면의 배경 선택지 목록
    @Operation(summary = "초대장 배경 색상 전체 조회", description = "초대장 꾸미기 화면의 배경 선택지 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<InvitationBackgroundResponse>> getAllBackgrounds() {
        return ApiResponse.onSuccess(InvitationSuccessCode.BACKGROUND_LIST_OK, invitationBackgroundService.getAllBackgrounds());
    }

    /** 배경 색상 생성 — 관리자 전용 */
    @AdminOnly
    @Operation(summary = "[관리자 전용] 초대장 배경 색상 생성",
            description = "관리자 계정만 호출할 수 있습니다. 일반 사용자 요청은 403(COMMON403_1)으로 차단됩니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // 응답 본문 코드(BACKGROUND201_1)와 실제 HTTP 상태를 201로 일치시킨다
    public ApiResponse<InvitationBackgroundCreateResponse> create(
            @Valid @RequestBody InvitationBackgroundRequest request) {
        return ApiResponse.onSuccess(InvitationSuccessCode.BACKGROUND_CREATE_OK, invitationBackgroundService.create(request));
    }

    /** 배경 색상 수정 — 관리자 전용 */
    @AdminOnly
    @Operation(summary = "[관리자 전용] 초대장 배경 색상 수정",
            description = "관리자 계정만 호출할 수 있습니다. 일반 사용자 요청은 403(COMMON403_1)으로 차단됩니다.")
    @PutMapping("/{id}")
    public ApiResponse<InvitationBackgroundResponse> update(@PathVariable Long id,
                                                            @Valid @RequestBody InvitationBackgroundRequest request) {
        return ApiResponse.onSuccess(InvitationSuccessCode.BACKGROUND_UPDATE_OK, invitationBackgroundService.update(id, request));
    }

    /** 배경 색상 삭제 — 관리자 전용, soft delete (엔티티 InvitationBackground.delete() 주석 참고) */
    @AdminOnly
    @Operation(summary = "[관리자 전용] 초대장 배경 색상 삭제",
            description = "관리자 계정만 호출할 수 있습니다. 일반 사용자 요청은 403(COMMON403_1)으로 차단됩니다. (Soft Delete)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        invitationBackgroundService.delete(id);
        return ApiResponse.onSuccess(InvitationSuccessCode.BACKGROUND_DELETE_OK, null);
    }
}
