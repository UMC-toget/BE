package com.example.toget.domain.user.dto;

import com.example.toget.domain.user.enums.BankName;

/** 계좌 단건 응답 — 목록 조회와 수정 응답에서 공용으로 사용 */
public record UserAccountResponse(
        Long userAccountId,
        BankName bankName, // enum은 JSON에서 "KAKAO_BANK" 같은 이름 문자열로 직렬화된다
        String accountOwner,
        String account
) {
}
