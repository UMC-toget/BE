package com.example.toget.domain.dev.controller;

import com.example.toget.domain.dev.dto.DevSetupResponse;
import com.example.toget.domain.dev.service.DevSetupService;
import com.example.toget.global.apiPayload.ApiResponse;
import com.example.toget.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "개발 전용 API", description = "로컬/개발 환경 전용 테스트 데이터 세팅 및 토큰 발급 API")
@Profile({"local", "dev"})
@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevSetupController {

    private final DevSetupService devSetupService;

    @Operation(
            summary = "테스트 데이터 세팅 및 토큰 일괄 발급",
            description = "개설자/공동관리자/참여자 3개 계정을 생성하고, 5개 상태(SELECTING, SETTLING, PURCHASING, DELIVERING, ENDED)의 함께선물 펀딩을 생성 및 멤버로 배정 후 역할별 토큰을 응답합니다."
    )
    @PostMapping("/setup-test-data")
    public ApiResponse<DevSetupResponse> setupTestData() {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, devSetupService.setupTestData());
    }
}
