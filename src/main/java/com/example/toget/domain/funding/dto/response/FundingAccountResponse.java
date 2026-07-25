package com.example.toget.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record FundingAccountResponse(
        @Schema(description = "사용자 계좌 ID", example = "5")
        Long userAccountId,

        @Schema(description = "은행명", example = "카카오뱅크")
        String bankName,

        @Schema(description = "계좌번호", example = "3333-12-3456789")
        String account,

        @Schema(description = "예금주", example = "홍길동")
        String accountOwner
) {}