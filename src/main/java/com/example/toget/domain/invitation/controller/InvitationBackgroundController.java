package com.example.toget.domain.invitation.controller;

import com.example.toget.domain.invitation.dto.InvitationBackgroundCreateResponse;
import com.example.toget.domain.invitation.dto.InvitationBackgroundRequest;
import com.example.toget.domain.invitation.dto.InvitationBackgroundResponse;
import com.example.toget.domain.invitation.service.InvitationBackgroundService;
import com.example.toget.global.apiPayload.ApiResponse;
import com.example.toget.global.apiPayload.code.GeneralSuccessCode;
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
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 초대장 배경 색상 CRUD API 컨트롤러.
 * 전체 조회(GET)는 비로그인 허용(SecurityConfig permitAll), 생성/수정/삭제는 로그인 필요.
 * TODO: ADMIN 권한 도입 후 생성/수정/삭제는 관리자 전용으로 제한 (SecurityConfig의 TODO 참고)
 */
@Tag(name = "초대장 API", description = "초대장 및 캐릭터 관련 API")
@RestController
@RequestMapping("/api/v1/invitation-backgrounds")
@RequiredArgsConstructor
public class InvitationBackgroundController {

    private final InvitationBackgroundService invitationBackgroundService;

    // 배경 색상 전체 조회 — 초대장 꾸미기 화면의 배경 선택지 목록
    @GetMapping
    public ApiResponse<List<InvitationBackgroundResponse>> getAllBackgrounds() {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, invitationBackgroundService.getAllBackgrounds());
    }

    // 배경 색상 생성
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // 응답 본문 코드(COMMON201_1)와 실제 HTTP 상태를 201로 일치시킨다
    public ApiResponse<InvitationBackgroundCreateResponse> create(
            @Valid @RequestBody InvitationBackgroundRequest request) {
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, invitationBackgroundService.create(request));
    }

    // 배경 색상 수정
    @PutMapping("/{id}")
    public ApiResponse<InvitationBackgroundResponse> update(@PathVariable Long id,
                                                            @Valid @RequestBody InvitationBackgroundRequest request) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, invitationBackgroundService.update(id, request));
    }

    // 배경 색상 삭제 — soft delete (엔티티 InvitationBackground.delete() 주석 참고)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        invitationBackgroundService.delete(id);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }
}
