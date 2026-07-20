package com.example.toget.domain.user.dto;

import com.example.toget.global.enums.BankName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 계좌 등록(POST) 요청 본문 — 생성이므로 모든 필드 필수 (수정은 UserAccountUpdateRequest 사용).
 * bankName은 enum이라 목록에 없는 은행명은 역직렬화 단계에서 이미 거부된다.
 * @NotNull(객체용)과 @NotBlank(문자열용: null+빈값+공백 거부)를 타입에 맞게 구분해 사용.
 */
public record UserAccountRequest(
        @Schema(description = "은행 이름", example = "KAKAO_BANK")
        @NotNull(message = "은행 이름은 필수입니다.")
        BankName bankName,

        @Schema(description = "예금주 명", example = "홍길동")
        @NotBlank(message = "예금주 명은 필수입니다.")
        @Size(max = 50, message = "예금주 명은 50자를 초과할 수 없습니다.")
        String accountOwner,

        @Schema(description = "계좌 번호 (하이픈 제외)", example = "3333011234567")
        @NotBlank(message = "계좌 번호는 필수입니다.")
        @Size(max = 50, message = "계좌 번호는 50자를 초과할 수 없습니다.")
        @Pattern(regexp = "^[0-9]+$", message = "계좌 번호는 숫자만 입력해야 합니다.")
        String account
) {
}
