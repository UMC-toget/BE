package com.example.toget.domain.workspace.dto;

import com.example.toget.global.enums.BankName;
import lombok.Builder;
import java.time.LocalDate;

@Builder
public record FundingTogetherDraftDetailResponse(
        Long togetherDraftsGiftId,
        Integer step,
        String title,
        String receiver,
        LocalDate anniversaryDate,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        String thumbnailImageUrl,
        AccountResponse account,
        String cardTitle,
        String cardContent
) {
    @Builder
    public record AccountResponse(
            Long userAccountId,
            BankName bankName,
            String bankAccount,
            String accountOwner
    ) {}
}
