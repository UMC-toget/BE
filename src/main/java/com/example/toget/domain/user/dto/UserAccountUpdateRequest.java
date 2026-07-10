package com.example.toget.domain.user.dto;

import com.example.toget.domain.user.enums.BankName;
import jakarta.validation.constraints.Size;

/**
 * 계좌 수정(PATCH) 요청 본문.
 * 생성용 UserAccountRequest와 달리 모든 필드가 선택 — 보낸 필드만 수정하는 부분 수정 의미론.
 * null(=안 보냄) 필드는 UserAccount.update에서 건너뛴다.
 */
public record UserAccountUpdateRequest(
        BankName bankName,

        @Size(max = 50, message = "예금주명은 50자 이하여야 합니다.")
        String accountOwner,

        @Size(max = 50, message = "계좌번호는 50자 이하여야 합니다.")
        String account
) {
}
