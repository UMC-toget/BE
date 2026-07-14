package com.example.toget.domain.workspace.dto;

import lombok.Builder;
import java.time.LocalDate;

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
        InvitationCardResponse invitationCard
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
}
