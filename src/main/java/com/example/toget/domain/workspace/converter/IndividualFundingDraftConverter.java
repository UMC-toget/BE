package com.example.toget.domain.workspace.converter;

import com.example.toget.domain.workspace.dto.IndividualFundingDraftDetailResponse;
import com.example.toget.domain.workspace.entity.IndividualFundingDraft;

public class IndividualFundingDraftConverter {

    public static IndividualFundingDraftDetailResponse toDetailResponse(IndividualFundingDraft draft) {
        return IndividualFundingDraftDetailResponse.builder()
                .id(draft.getId())
                .step(draft.getStep())
                .title(draft.getTitle())
                .anniversaryDate(draft.getAnniversaryDate())
                .endDate(draft.getEndDate())
                .greeting(draft.getGreeting())
                .thumbnailUrl(draft.getThumbnailUrl())
                .userAccountId(draft.getUserAccountId())
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
                .build();
    }
}
