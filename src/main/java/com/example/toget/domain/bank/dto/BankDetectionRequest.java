package com.example.toget.domain.bank.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "계좌번호 기반 은행 추론 요청 DTO")
public record BankDetectionRequest(
        @Schema(description = "추론할 계좌번호 (하이픈 포함/미포함)", example = "3333-01-1234567")
        @NotBlank(message = "계좌번호는 필수 입력 항목입니다.")
        @Pattern(regexp = "^[0-9-]+$", message = "계좌번호는 숫자와 하이픈(-)만 포함할 수 있습니다.")
        String accountNumber
) {
}
