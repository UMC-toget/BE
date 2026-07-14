package com.example.toget.domain.workspace.dto;

import lombok.Builder;
import java.time.LocalDate;
import java.util.List;

@Builder
public record IndividualFundingDraftDetailResponse(
        Long id,
        Integer step,
        String title,
        LocalDate anniversaryDate,
        LocalDate startDate,
        LocalDate endDate,
        String greeting,
        String thumbnailUrl,
        Long userAccountId,
        VisibilitySettingsResponse visibilitySettings,
        InvitationCardResponse invitationCard,
        List<DraftGiftResponse> gifts
) {

    @Builder
    public record VisibilitySettingsResponse(
            Boolean isProgressPublic,
            Boolean isAmountPublic,
            Boolean isParticipantCountPublic,
            Boolean isParticipantNamePublic,
            Boolean isMessagePublic
    ) {}

    @Builder
    public record InvitationCardResponse(
            Long characterId,
            Long backgroundId,
            String title,
            String content
    ) {}

    @Builder
    public record DraftGiftResponse(
            String giftName,
            Long giftPrice,
            String giftShopUrl,
            String giftImageUrl
    ) {}
}
