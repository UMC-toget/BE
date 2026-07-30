package com.example.toget.domain.funding.dto.response;

public record FundingReviewInvitationResponse(
        String invitationTitle,
        String invitationContent,
        Long invitationCharacterId,
        Long invitationBackgroundId
) {}