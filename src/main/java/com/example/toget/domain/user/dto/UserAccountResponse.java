package com.example.toget.domain.user.dto;

import com.example.toget.global.enums.BankName;
import io.swagger.v3.oas.annotations.media.Schema;

/** 계좌 단건 응답 — 목록 조회와 수정 응답에서 공용으로 사용 */
public record UserAccountResponse(
        @Schema(description = "사용자 계좌 ID", example = "1")
        Long userAccountId,

        @Schema(description = "은행 이름", example = "KAKAO_BANK")
        BankName bankName, // enum은 JSON에서 "KAKAO_BANK" 같은 이름 문자열로 직렬화된다

        @Schema(description = "예금주 명", example = "홍길동")
        String accountOwner,

        @Schema(description = "계좌 번호 (하이픈 제외)", example = "3333011234567")
        String account
) {
}
