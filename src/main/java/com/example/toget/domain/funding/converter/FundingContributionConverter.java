package com.example.toget.domain.funding.converter;

import com.example.toget.domain.funding.dto.response.ContributionResponse;
import com.example.toget.domain.funding.dto.response.FundingContributionCreateResponse;
import com.example.toget.domain.funding.dto.response.FundingContributionDetailResponse;
import com.example.toget.domain.funding.dto.response.FundingContributionRollingPaperResponse;
import com.example.toget.domain.funding.entity.FundingContribution;

import java.util.List;

public class FundingContributionConverter {

    public static ContributionResponse toResponse(FundingContribution contribution) {
        return new ContributionResponse(
                contribution.getId(),
                contribution.getGuestName(),      // 필드명 매핑: guestName → senderName
                contribution.getAmount(),
                contribution.getContent(),
                contribution.getIsAnonymous(),
                !contribution.getIsMessageVisible(),  // isMessageVisible=true → isPrivate=false
                contribution.getBackgroundId()
        );
    }


    public static FundingContributionCreateResponse toCreateResponse(FundingContribution contribution) {
        return new FundingContributionCreateResponse(contribution.getFundingId());
    }

    public static FundingContributionRollingPaperResponse toRollingPaperResponse(
            List<FundingContribution> contributions, boolean isOwner
    ) {
        List<FundingContributionRollingPaperResponse.ContributionItem> items = contributions.stream()
                .map(c -> toListItem(c, isOwner))
                .toList();

        return new FundingContributionRollingPaperResponse(contributions.size(), items);
    }

    public static FundingContributionRollingPaperResponse.ContributionItem toListItem(
            FundingContribution c, boolean isOwner
    ) {
        boolean hideSender = !isOwner && Boolean.TRUE.equals(c.getIsAnonymous());
        boolean hideContent = shouldHideContent(c, isOwner);
        boolean isPrivate = !c.getIsMessageVisible();

        return new FundingContributionRollingPaperResponse.ContributionItem(
                c.getId(),
                hideSender ? null : c.getGuestName(),
                c.getIsAnonymous(),
                c.getAmount(),
                hideContent ? null : c.getContent(),
                isPrivate,
                c.getCreatedAt()
        );
    }

    public static FundingContributionDetailResponse toDetailResponse(FundingContribution contribution, boolean isOwner) {
        String content = shouldHideContent(contribution, isOwner) ? null : contribution.getContent();
        return new FundingContributionDetailResponse(contribution.getId(), content);
    }

    public static boolean shouldHideContent(FundingContribution c, boolean isOwner) {
        return !isOwner && !c.getIsMessageVisible();
    }
}