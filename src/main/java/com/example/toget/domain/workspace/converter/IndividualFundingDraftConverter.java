package com.example.toget.domain.workspace.converter;

import com.example.toget.domain.gift.entity.IndividualFundingDraftGift;
import com.example.toget.domain.user.entity.UserAccount;
import com.example.toget.domain.workspace.dto.IndividualFundingDraftDetailResponse;
import com.example.toget.domain.workspace.entity.IndividualFundingDraft;
import java.util.List;

public class IndividualFundingDraftConverter {

    public static IndividualFundingDraftDetailResponse toDetailResponse(
            IndividualFundingDraft draft,
            List<IndividualFundingDraftGift> gifts,
            UserAccount userAccount
    ) {
        return IndividualFundingDraftDetailResponse.builder()
                .id(draft.getId())
                .step(draft.getStep())
                .title(draft.getTitle())
                .anniversaryDate(draft.getAnniversaryDate())
                .startDate(draft.getStartDate())
                .endDate(draft.getEndDate())
                .greeting(draft.getGreeting())
                .thumbnailUrl(draft.getThumbnailUrl())
                .account(userAccount != null ? IndividualFundingDraftDetailResponse.AccountResponse.builder()
                        .userAccountId(userAccount.getId())
                        .bankName(userAccount.getBankName())
                        .bankAccount(userAccount.getAccount())
                        .accountOwner(userAccount.getAccountOwner())
                        .build() : null)
                .visibilitySettings(IndividualFundingDraftDetailResponse.VisibilitySettingsResponse.builder()
                        .isProgressPublic(draft.getIsProgressPublic())
                        .isAmountPublic(draft.getIsAmountPublic())
                        .isParticipantCountPublic(draft.getIsParticipantCountPublic())
                        .isParticipantNamePublic(draft.getIsParticipantNamePublic())
                        .isMessagePublic(draft.getIsMessagePublic())
                        .build())
                .invitationCard(IndividualFundingDraftDetailResponse.InvitationCardResponse.builder()
                        .characterId(draft.getInvitationCharacterId() != null ? draft.getInvitationCharacterId().getId() : null)
                        .backgroundId(draft.getInvitationBackgroundId() != null ? draft.getInvitationBackgroundId().getId() : null)
                        .title(draft.getInvitationTitle())
                        .content(draft.getInvitationContent())
                        .build())
                .gifts(gifts.stream()
                        .map(gift -> IndividualFundingDraftDetailResponse.DraftGiftResponse.builder()
                                .giftName(gift.getName())
                                .giftPrice(gift.getPrice())
                                .giftShopUrl(gift.getPurchaseUrl())
                                .giftImageUrl(gift.getImageUrl())
                                .build())
                        .toList())
                .build();
    }
}
