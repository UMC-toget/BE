package com.example.toget.domain.bank.dto;

import com.example.toget.global.enums.BankName;
import io.swagger.v3.oas.annotations.media.Schema;

/** 은행 단건 응답 — 목록 조회와 관리자 수정 응답에서 공용으로 사용 */
public record BankResponse(
        @Schema(description = "은행 ID", example = "9")
        Long bankId,

        @Schema(description = "은행 코드 — 계좌 등록 시 bankName으로 그대로 전달하면 된다", example = "KAKAO_BANK")
        BankName code, // enum은 JSON에서 "KAKAO_BANK" 같은 이름 문자열로 직렬화된다

        @Schema(description = "은행 표시명", example = "카카오뱅크")
        String displayName,

        @Schema(description = "은행 아이콘 URL — 아이콘 미확보 은행은 null이므로 fallback 처리 필요",
                example = "https://toget-bucket-dev.s3.ap-northeast-2.amazonaws.com/bank-icons/v1/KAKAO_BANK.svg")
        String iconUrl
) {
}
