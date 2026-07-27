package com.example.toget.domain.funding.converter;

import com.example.toget.domain.funding.dto.response.*;
import com.example.toget.domain.funding.entity.FundingContribution;
import com.example.toget.domain.funding.exception.FundingException;
import com.example.toget.domain.funding.exception.code.FundingErrorCode;
import com.example.toget.domain.user.entity.User;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Map;

public class FundingContributionConverter {

    public static ContributionResponse toResponse(FundingContribution contribution, User user) {
        // TODO: 실제 사용 시점에 호출부에서 로그인 유저는 userRepository로 조회해 user를 채워서 호출할 것
        String senderName = (user != null) ? user.getName() : contribution.getGuestName();

        return new ContributionResponse(
                contribution.getId(),
                contribution.getGuestName(),
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

    public static FundingContributionListResponse.ContributionItem toContributionListItem(
            FundingContribution c, Map<Long, User> userMap
    ) {
        String senderName;
        String profileImageUrl;
        if (c.getUserId() != null) {
            User user = userMap.get(c.getUserId());
            if (user == null) {
                throw new FundingException(FundingErrorCode.MEMBER_NOT_FOUND);
            }
            senderName = user.getName();
            profileImageUrl = user.getProfileImageUrl();
        } else {
            senderName = c.getGuestName();
            profileImageUrl = null;
        }

        return new FundingContributionListResponse.ContributionItem(
                c.getId(), senderName, profileImageUrl, c.getAmount(), c.getCreatedAt()
        );
    }

    public static FundingContributionListResponse toContributionListResponse(
            Slice<FundingContribution> slice, Map<Long, User> userMap,
            int participantCount, Long totalAmount, int page, int size
    ) {
        List<FundingContributionListResponse.ContributionItem> items = slice.getContent().stream()
                .map(c -> toContributionListItem(c, userMap))
                .toList();

        return new FundingContributionListResponse(
                participantCount, totalAmount, items, page, size, slice.hasNext()
        );
    }
}