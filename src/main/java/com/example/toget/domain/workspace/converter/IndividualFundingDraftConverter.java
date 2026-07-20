package com.example.toget.domain.workspace.converter;

import com.example.toget.domain.gift.entity.IndividualFundingDraftGift;
import com.example.toget.domain.invitation.entity.CharacterEntity;
import com.example.toget.domain.invitation.entity.InvitationBackground;
import com.example.toget.domain.user.entity.UserAccount;
import com.example.toget.domain.workspace.dto.IndividualFundingDraftDetailResponse;
import com.example.toget.domain.workspace.dto.IndividualFundingDraftSaveRequest;
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
                        .characterId(draft.getInvitationCharacter() != null ? draft.getInvitationCharacter().getId() : null)
                        .backgroundId(draft.getInvitationBackground() != null ? draft.getInvitationBackground().getId() : null)
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

    public static IndividualFundingDraft toEntity(
            Long userId,
            IndividualFundingDraftSaveRequest request,
            CharacterEntity character,
            InvitationBackground background,
            Long userAccountId
    ) {
        Boolean isProgressPublic = (request.visibilitySettings() != null && request.visibilitySettings().isProgressPublic() != null) ? request.visibilitySettings().isProgressPublic() : true;
        Boolean isAmountPublic = (request.visibilitySettings() != null && request.visibilitySettings().isAmountPublic() != null) ? request.visibilitySettings().isAmountPublic() : true;
        Boolean isParticipantCountPublic = (request.visibilitySettings() != null && request.visibilitySettings().isParticipantCountPublic() != null) ? request.visibilitySettings().isParticipantCountPublic() : true;
        Boolean isParticipantNamePublic = (request.visibilitySettings() != null && request.visibilitySettings().isParticipantNamePublic() != null) ? request.visibilitySettings().isParticipantNamePublic() : true;
        Boolean isMessagePublic = (request.visibilitySettings() != null && request.visibilitySettings().isMessagePublic() != null) ? request.visibilitySettings().isMessagePublic() : true;

        String invitationTitle = request.invitationCard() != null ? request.invitationCard().title() : null;
        String invitationContent = request.invitationCard() != null ? request.invitationCard().content() : null;

        return IndividualFundingDraft.builder()
                .userId(userId)
                .step(request.step())
                .title(request.title())
                .anniversaryDate(request.anniversaryDate())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .greeting(request.greeting())
                .thumbnailUrl(request.thumbnailUrl())
                .isProgressPublic(isProgressPublic)
                .isAmountPublic(isAmountPublic)
                .isParticipantCountPublic(isParticipantCountPublic)
                .isParticipantNamePublic(isParticipantNamePublic)
                .isMessagePublic(isMessagePublic)
                .invitationCharacter(character)
                .invitationBackground(background)
                .invitationTitle(invitationTitle)
                .invitationContent(invitationContent)
                .userAccountId(userAccountId)
                .build();
    }

    public static IndividualFundingDraftGift toDraftGiftEntity(
            Long draftId,
            IndividualFundingDraftSaveRequest.DraftGiftRequest giftReq
    ) {
        return IndividualFundingDraftGift.builder()
                .myDraftId(draftId)
                .name(giftReq.giftName())
                .price(giftReq.giftPrice())
                .purchaseUrl(giftReq.giftShopUrl())
                .imageUrl(giftReq.giftImageUrl())
                .build();
    }
}
