package com.example.toget.domain.workspace.converter;

import com.example.toget.domain.user.entity.UserAccount;
import com.example.toget.domain.workspace.dto.FundingTogetherDraftDetailResponse;
import com.example.toget.domain.workspace.dto.FundingTogetherDraftSaveRequest;
import com.example.toget.domain.workspace.entity.FundingTogetherDraft;

public class FundingTogetherDraftConverter {

    public static FundingTogetherDraftDetailResponse toDetailResponse(
            FundingTogetherDraft draft,
            UserAccount userAccount
    ) {
        return FundingTogetherDraftDetailResponse.builder()
                .togetherDraftsGiftId(draft.getId())
                .step(draft.getStep())
                .title(draft.getTitle())
                .receiver(draft.getReceiver())
                .anniversaryDate(draft.getAnniversaryDate())
                .startDate(draft.getStartDate())
                .endDate(draft.getEndDate())
                .description(draft.getDescription())
                .thumbnailImageUrl(draft.getThumbnailImageUrl())
                .account(userAccount != null ? FundingTogetherDraftDetailResponse.AccountResponse.builder()
                        .userAccountId(userAccount.getId())
                        .bankName(userAccount.getBankName())
                        .bankAccount(userAccount.getAccount())
                        .accountOwner(userAccount.getAccountOwner())
                        .build() : null)
                .cardTitle(draft.getCardTitle())
                .cardContent(draft.getCardContent())
                .build();
    }

    public static FundingTogetherDraft toEntity(
            Long userId,
            FundingTogetherDraftSaveRequest request,
            Long userAccountId
    ) {
        return FundingTogetherDraft.builder()
                .userId(userId)
                .step(request.step())
                .title(request.title())
                .receiver(request.receiver())
                .anniversaryDate(request.anniversaryDate())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .description(request.description())
                .thumbnailImageUrl(request.thumbnailImageUrl())
                .userAccountId(userAccountId)
                .cardTitle(request.cardTitle())
                .cardContent(request.cardContent())
                .build();
    }
}
