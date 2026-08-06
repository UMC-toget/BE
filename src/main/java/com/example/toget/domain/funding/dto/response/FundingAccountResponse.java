package com.example.toget.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record FundingAccountResponse(
        @Schema(description = "사용자 계좌 ID", example = "5")
        Long userAccountId,

        // 실제로 내려가는 값은 표시명이 아니라 BankName enum의 이름 문자열이다.
        // (기존 example이 "카카오뱅크"로 되어 있어 실제 응답과 어긋나 있었다 — 이번에 바로잡음)
        @Schema(description = "은행 코드", example = "KAKAO_BANK")
        String bankName,

        @Schema(description = "은행 표시명", example = "카카오뱅크")
        String bankDisplayName,

        @Schema(description = "은행 아이콘 URL — 아이콘 미확보 은행은 null이므로 fallback 처리 필요",
                example = "https://toget-bucket-dev.s3.ap-northeast-2.amazonaws.com/bank-icons/v1/KAKAO_BANK.svg")
        String bankIconUrl,

        @Schema(description = "계좌번호", example = "3333-12-3456789")
        String account,

        @Schema(description = "예금주", example = "홍길동")
        String accountOwner
) {}