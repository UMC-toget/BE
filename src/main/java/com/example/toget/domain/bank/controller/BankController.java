package com.example.toget.domain.bank.controller;

import com.example.toget.domain.bank.dto.BankResponse;
import com.example.toget.domain.bank.dto.BankUpdateRequest;
import com.example.toget.domain.bank.exception.code.BankSuccessCode;
import com.example.toget.domain.bank.service.BankService;
import com.example.toget.global.annotation.AdminOnly;
import com.example.toget.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 은행 마스터 데이터 API 컨트롤러.
 * 목록 조회(GET)는 비로그인 허용(SecurityConfig permitAll),
 * 수정은 관리자 전용(@AdminOnly → AdminOnlyInterceptor).
 *
 * <p>캐릭터(CharacterController)·초대장 배경과 동일한 "공통 리소스 카탈로그" 구조다.
 *
 * <p>[생성/삭제 API가 없는 이유]
 * banks의 행은 BankSeeder가 BankName enum을 순회해서만 만든다. 임의로 은행을 추가하면
 * 목록에는 보이지만 계좌 등록 요청은 enum 역직렬화 단계에서 거부되어 400이 난다.
 * 은행 추가는 반드시 enum에 상수를 넣는 경로로만 이뤄져야 하므로 POST/DELETE를 열지 않았다.
 * 영업 중단 은행은 삭제 대신 isActive=false로 목록에서 감춘다.
 */
@Tag(name = "은행 API", description = "은행 목록 조회 및 관리 API")
@RestController
@RequestMapping("/api/v1/banks")
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;

    @Operation(summary = "은행 목록 조회",
            description = "계좌 등록·수정 화면의 은행 선택지 목록을 조회합니다. 노출 순서(sortOrder) 오름차순이며, "
                    + "비활성 은행은 제외됩니다. iconUrl은 아이콘 미확보 은행의 경우 null일 수 있으므로 "
                    + "프론트에서 fallback 아이콘 처리가 필요합니다.")
    @GetMapping
    public ApiResponse<List<BankResponse>> getBanks() {
        return ApiResponse.onSuccess(BankSuccessCode.BANK_LIST_OK, bankService.getActiveBanks());
    }

    /** 은행 정보 수정 — 관리자 전용. 아이콘 교체를 코드 배포 없이 처리하기 위한 API */
    @AdminOnly
    @Operation(summary = "[관리자 전용] 은행 정보 수정",
            description = "관리자 계정만 호출할 수 있습니다. 일반 사용자 요청은 403(COMMON403_1)으로 차단됩니다. "
                    + "보낸 필드만 수정되며, 은행 코드(code)는 기존 계좌와의 매핑이 깨지므로 수정할 수 없습니다.")
    @PatchMapping("/{bankId}")
    public ApiResponse<BankResponse> update(@PathVariable Long bankId,
                                            @Valid @RequestBody BankUpdateRequest request) {
        return ApiResponse.onSuccess(BankSuccessCode.BANK_UPDATE_OK, bankService.update(bankId, request));
    }
}
