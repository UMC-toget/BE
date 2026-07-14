package com.example.toget.domain.workspace.dto;

import java.time.LocalDate;

public record IndividualFundingDraftDetailResponse(
        Long id,
        Integer step,
        String title,
        LocalDate anniversaryDate,
        LocalDate endDate,
        String greeting,
        String thumbnailUrl,
        Long userAccountId,
        VisibilitySettingsResponse visibilitySettings,
        InvitationCardResponse invitationCard
) {

    public record VisibilitySettingsResponse(
            Boolean isProgressPublic,
            Boolean isAmountPublic,
            Boolean isParticipantCountPublic,
            Boolean isParticipantNamePublic,
            Boolean isMessagePublic
    ) {}

    public record InvitationCardResponse(
            Long characterId,
            Long backgroundId,
            String title,
            String content
    ) {}
}
