package com.example.toget.domain.funding.converter;

import com.example.toget.domain.funding.dto.response.*;
import com.example.toget.domain.funding.entity.FundingContribution;
import com.example.toget.domain.funding.entity.FundingVisibilitySettings;
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
            List<FundingContribution> contributions, boolean isOwner, FundingVisibilitySettings visibility
    ) {
        List<FundingContributionRollingPaperResponse.ContributionItem> items = contributions.stream()
                .map(c -> toListItem(c, isOwner, visibility))
                .toList();

        return new FundingContributionRollingPaperResponse(contributions.size(), items);
    }

    public static FundingContributionRollingPaperResponse.ContributionItem toListItem(
            FundingContribution c, boolean isOwner, FundingVisibilitySettings visibility
    ) {
        boolean hideSender = shouldHideSender(c, isOwner, visibility);
        boolean hideContent = shouldHideContent(c, isOwner, visibility);
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

    public static FundingContributionDetailResponse toDetailResponse(
            FundingContribution contribution, boolean isOwner, FundingVisibilitySettings visibility
    ) {
        String content = shouldHideContent(contribution, isOwner, visibility) ? null : contribution.getContent();
        return new FundingContributionDetailResponse(contribution.getId(), content);
    }

    /**
     * 보낸 사람 이름을 가릴지 판단한다.
     *
     * [설계 포인트]
     *  - 참여자 개인의 익명 선택과 개설자의 공개 설정을 OR로 결합한다.
     *    둘 중 하나라도 "가리라"고 하면 가린다. 한쪽이 공개를 택했다고 다른 쪽 의사를 덮어쓰지 않는다.
     *  - visibility가 null이면 공개 설정이 없는 펀딩(TOGETHER_GIFT 등)이므로 전체 공개로 간주한다.
     *  - 개설자 본인(isOwner)은 항상 원본을 본다. 자기 펀딩의 참여 현황을 파악해야 하기 때문.
     */
    public static boolean shouldHideSender(
            FundingContribution c, boolean isOwner, FundingVisibilitySettings visibility
    ) {
        if (isOwner) {
            return false;
        }
        boolean hiddenByParticipant = Boolean.TRUE.equals(c.getIsAnonymous());
        boolean hiddenByOwnerSetting = visibility != null
                && Boolean.FALSE.equals(visibility.getIsParticipantNameVisible());
        return hiddenByParticipant || hiddenByOwnerSetting;
    }

    /**
     * 축하 메시지 내용을 가릴지 판단한다. 판단 규칙은 shouldHideSender와 동일하다.
     *
     * [설계 포인트]
     *  - 목록(toListItem)과 상세(toDetailResponse)가 같은 메서드를 쓴다.
     *    목록만 가리면 contributionId를 아는 방문자가 상세 API로 우회해 내용을 읽을 수 있다.
     */
    public static boolean shouldHideContent(
            FundingContribution c, boolean isOwner, FundingVisibilitySettings visibility
    ) {
        if (isOwner) {
            return false;
        }
        boolean hiddenByParticipant = !c.getIsMessageVisible();
        boolean hiddenByOwnerSetting = visibility != null
                && Boolean.FALSE.equals(visibility.getIsMessageVisible());
        return hiddenByParticipant || hiddenByOwnerSetting;
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