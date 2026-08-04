package com.example.toget.domain.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 은행 정보 수정(PATCH) 요청 본문 — 관리자 전용.
 * 모든 필드가 선택이며, 보낸 필드만 수정하는 부분 수정 의미론.
 * null(=안 보냄) 필드는 Bank.update에서 건너뛴다.
 *
 * <p>은행 코드(code)는 수정 대상이 아니다. 코드는 BankName enum과 1:1로 묶여 있어
 * 바꾸는 순간 기존 계좌(user_accounts.bank_name)와 매핑이 깨지기 때문이다.
 */
public record BankUpdateRequest(
        @Schema(description = "변경할 표시명", example = "카카오뱅크")
        @Size(max = 50, message = "은행 표시명은 50자를 초과할 수 없습니다.")
        String displayName,

        @Schema(description = "변경할 아이콘 URL", example = "https://toget-bucket-dev.s3.ap-northeast-2.amazonaws.com/bank-icons/v2/KAKAO_BANK.svg")
        @Size(max = 500, message = "아이콘 URL은 500자를 초과할 수 없습니다.")
        String iconUrl,

        @Schema(description = "변경할 노출 순서 (오름차순)", example = "9")
        @PositiveOrZero(message = "노출 순서는 0 이상이어야 합니다.")
        Integer sortOrder,

        @Schema(description = "노출 여부 — false로 두면 신규 계좌 등록 시 선택할 수 없다", example = "true")
        Boolean isActive
) {
}
