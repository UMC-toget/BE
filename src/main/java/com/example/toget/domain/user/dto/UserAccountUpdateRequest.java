package com.example.toget.domain.user.dto;

import com.example.toget.global.enums.BankName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 계좌 수정(PATCH) 요청 본문.
 * 생성용 UserAccountRequest와 달리 모든 필드가 선택 — 보낸 필드만 수정하는 부분 수정 의미론.
 * null(=안 보냄) 필드는 UserAccount.update에서 건너뛴다.
 */
public record UserAccountUpdateRequest(
        @Schema(description = "변경할 은행 이름", example = "KB")
        BankName bankName,

        @Schema(description = "변경할 예금주 명", example = "임꺽정")
        @Size(max = 50, message = "예금주명은 50자 이하여야 합니다.")
        String accountOwner,

        @Schema(description = "변경할 계좌 번호 (하이픈 제외)", example = "3021234567891")
        @Size(max = 50, message = "계좌번호는 50자 이하여야 합니다.")
        @Pattern(regexp = "^[0-9]+$", message = "계좌 번호는 숫자만 입력해야 합니다.")
        String account
) {
}
