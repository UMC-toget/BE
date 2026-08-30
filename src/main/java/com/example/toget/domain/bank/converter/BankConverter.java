package com.example.toget.domain.bank.converter;

import com.example.toget.domain.bank.dto.BankDetectionResponse;
import com.example.toget.domain.bank.dto.BankResponse;
import com.example.toget.domain.bank.entity.Bank;
import com.example.toget.global.enums.BankName;

/** Bank 엔티티 → 은행 응답 DTO 변환 (다른 도메인의 Converter와 같은 역할의 변환 전담 클래스) */
public class BankConverter {

    private BankConverter() {
    }

    public static BankResponse toResponse(Bank bank) {
        return new BankResponse(bank.getId(), bank.getCode(), bank.getDisplayName(), bank.getIconUrl());
    }

    public static BankDetectionResponse toDetectionResponse(BankName bankName, Bank bank) {
        return BankDetectionResponse.builder()
                .bankName(bankName)
                .displayName(bank != null ? bank.getDisplayName() : bankName.getDisplayName())
                .iconUrl(bank != null ? bank.getIconUrl() : null)
                .build();
    }
}
