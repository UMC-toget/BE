package com.example.toget.domain.user.dto;

import com.example.toget.global.enums.BankName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 계좌 등록(POST) 요청 본문 — 생성이므로 모든 필드 필수 (수정은 UserAccountUpdateRequest 사용).
 * bankName은 enum이라 목록에 없는 은행명은 역직렬화 단계에서 이미 거부된다.
 * @NotNull(객체용)과 @NotBlank(문자열용: null+빈값+공백 거부)를 타입에 맞게 구분해 사용.
 */
public record UserAccountRequest(
        @NotNull(message = "bankName은 필수입니다.")
        BankName bankName,

        @NotBlank(message = "accountOwner는 필수입니다.")
        @Size(max = 50) // 엔티티 컬럼 length와 맞춰 DB 오류 전에 400으로 거른다
        String accountOwner,

        @NotBlank(message = "account는 필수입니다.")
        @Size(max = 50)
        String account
) {
}
