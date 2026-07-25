package com.example.toget.domain.funding.converter;

import com.example.toget.domain.funding.dto.response.ContributionResponse;
import com.example.toget.domain.funding.entity.FundingContribution;

public class FundingContributionConverter {

    public static ContributionResponse toResponse(FundingContribution contribution) {
        return new ContributionResponse(
                contribution.getId(),
                contribution.getGuestName(),      // 필드명 매핑: guestName → senderName
                contribution.getAmount(),
                contribution.getContent(),
                contribution.getIsAnonymous(),
                !contribution.getIsMessageVisible(),  // ⚠️ isMessageVisible=true → isPrivate=false
                contribution.getBackgroundId()
        );
    }
}