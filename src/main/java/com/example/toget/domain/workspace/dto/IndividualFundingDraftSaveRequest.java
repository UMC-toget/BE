package com.example.toget.domain.workspace.dto;

import java.time.LocalDate;
import java.util.List;

public record IndividualFundingDraftSaveRequest(
        Integer step,
        String title,
        LocalDate anniversaryDate,
        LocalDate startDate,
        LocalDate endDate,
        String greeting,
        String thumbnailUrl,
        Long userAccountId,
        VisibilitySettingsRequest visibilitySettings,
        InvitationCardRequest invitationCard,
        List<DraftGiftRequest> gifts
) {

    public record VisibilitySettingsRequest(
            Boolean isProgressPublic,
            Boolean isAmountPublic,
            Boolean isParticipantCountPublic,
            Boolean isParticipantNamePublic,
            Boolean isMessagePublic
    ) {}

    public record InvitationCardRequest(
            Long characterId,
            Long backgroundId,
            String title,
            String content
    ) {}

    public record DraftGiftRequest(
            String giftName,
            Long giftPrice,
            String giftShopUrl,
            String giftImageUrl
    ) {}
}
