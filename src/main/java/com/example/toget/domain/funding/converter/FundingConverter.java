package com.example.toget.domain.funding.converter;

import com.example.toget.domain.funding.dto.MyFundingListResponse;
import com.example.toget.domain.funding.dto.MyFundingListResponse.MyFundingSummary;
import com.example.toget.domain.funding.dto.response.*;
import com.example.toget.domain.funding.entity.*;
import com.example.toget.domain.funding.enums.FundingRole;
import com.example.toget.domain.gift.entity.FundingGift;
import com.example.toget.domain.user.entity.User;
import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.exception.FundingException;
import com.example.toget.domain.funding.exception.code.FundingErrorCode;
import com.example.toget.domain.funding.dto.response.FundingAccountResponse;
import com.example.toget.domain.funding.dto.response.FundingAccountUpdateResponse;
import com.example.toget.domain.funding.dto.response.FundingBasicInfoResponse;
import com.example.toget.domain.funding.dto.response.FundingCreateResponse;
import com.example.toget.domain.user.entity.UserAccount;

import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Funding 엔티티 → 응답 DTO 변환 전담 클래스.
 * collectedAmount는 엔티티에 없는 계산 값이라 합산 맵을 함께 받아 조립한다.
 * 상태가 없으므로 static 메서드로만 구성 (private 생성자로 인스턴스화 차단) — UserConverter와 동일한 규칙.
 */
public class FundingConverter {

    private static final int PERCENT = 100;

    private FundingConverter() {
    }

    public static MyFundingListResponse toMyFundingListResponse(Slice<Funding> fundings,
                                                                Map<Long, Long> collectedAmounts,
                                                                Set<Long> reviewedFundingIds) {
        List<MyFundingSummary> items = fundings.getContent().stream()
                .map(funding -> toMyFundingSummary(
                        funding,
                        collectedAmounts.getOrDefault(funding.getId(), 0L), // 참여금 없는 펀딩은 0
                        reviewedFundingIds != null && reviewedFundingIds.contains(funding.getId())
                ))
                .toList();
        return new MyFundingListResponse(
                items,
                fundings.getNumber(),  // 현재 페이지 번호
                fundings.getSize(),    // 요청한 페이지 크기
                fundings.hasNext()
        );
    }

    private static MyFundingSummary toMyFundingSummary(Funding funding, Long collectedAmount, boolean hasReview) {
        return new MyFundingSummary(
                funding.getId(),
                funding.getFundingType().name(),
                funding.getTitle(),
                funding.getRecipientName(),
                funding.getTargetAmount(),
                collectedAmount,
                calculateProgressRate(collectedAmount, funding.getTargetAmount()),
                funding.getStatus().name(),
                funding.getEndDate(),
                funding.getThumbnailImageUrl(),
                funding.getCreatedAt(),
                hasReview
        );
    }

    /**
     * 소수점 내림(정수%), 100 초과 허용(초과 달성 그대로 노출)
     * targetAmount는 0 허용 -> 0 나눗셈을 방어
     */
    public static double calculateProgressRate(Long collectedAmount, Long targetAmount) {
        if (targetAmount == null || targetAmount == 0) {
            return 0.0;
        }
        if (collectedAmount == null) {
            return 0.0;
        }
        return Math.round(collectedAmount * 10000.0 / targetAmount) / 100.0;
    }

    public static FundingMemberManagementResponse toMemberManagementResponse(List<FundingMember> members, Map<Long, User> userMap) {
        List<FundingMemberManagementResponse.MemberInfo> admins = members.stream()
                .filter(m -> m.getRole() == FundingRole.CREATOR || m.getRole() == FundingRole.ADMIN)
                .map(m -> toMemberInfo(m, userMap))
                .toList();

        List<FundingMemberManagementResponse.MemberInfo> participants = members.stream()
                .filter(m -> m.getRole() == FundingRole.PARTICIPANT)
                .map(m -> toMemberInfo(m, userMap))
                .toList();

        return new FundingMemberManagementResponse(
                members.size(), admins.size(), participants.size(), admins, participants
        );
    }



    public static FundingSettlementListResponse.SettlementInfo toSettlementInfo(FundingMember member, Map<Long, User> userMap) {
        User user = userMap.get(member.getUserId());
        if (user == null) {
            throw new FundingException(FundingErrorCode.MEMBER_NOT_FOUND);
        }
        return new FundingSettlementListResponse.SettlementInfo(
                member.getId(), member.getUserId(), user.getName(), user.getProfileImageUrl(),
                member.getAmountDue(), member.getSettlementStatus().name()
        );
    }

    public static FundingMemberManagementResponse.MemberInfo toMemberInfo(FundingMember member, Map<Long, User> userMap) {
        User user = userMap.get(member.getUserId());
        if (user == null) {
            throw new FundingException(FundingErrorCode.MEMBER_NOT_FOUND);
        }
        return new FundingMemberManagementResponse.MemberInfo(
                member.getId(), member.getUserId(), user.getName(), user.getProfileImageUrl(), member.getRole().name()
        );
    }

    public static FundingSettlementListResponse toSettlementListResponse(List<FundingMember> settlementMembers, Map<Long, User> userMap) {
        long totalAmount = settlementMembers.stream()
                .mapToLong(FundingMember::getAmountDue)
                .sum();

        List<FundingSettlementListResponse.SettlementInfo> settlements = settlementMembers.stream()
                .map(m -> toSettlementInfo(m, userMap))
                .toList();

        return new FundingSettlementListResponse(settlementMembers.size(), totalAmount, settlements);
    }

    public static FundingCreateResponse toCreateResponse(Funding funding) {
        return new FundingCreateResponse(funding.getId());
    }

    public static FundingBasicInfoResponse toBasicInfoResponse(Funding funding) {
        return new FundingBasicInfoResponse(
                funding.getId(),
                funding.getTitle(),
                funding.getAnniversaryDate(),
                funding.getStartDate(),
                funding.getEndDate(),
                funding.getIntroduction(),
                funding.getThumbnailImageUrl()
        );
    }

    public static FundingAccountResponse toAccountResponse(UserAccount account) {
        return new FundingAccountResponse(
                account.getId(),
                account.getBankName().name(),
                // 아이콘·표시명은 엔티티의 널가드 메서드를 거친다 — 백필 전 레거시 행은 bank가 null이다
                account.getBankDisplayName(),
                account.getBankIconUrl(),
                account.getAccount(),
                account.getAccountOwner()
        );
    }

    public static FundingAccountUpdateResponse toAccountUpdateResponse(Funding funding) {
        return new FundingAccountUpdateResponse(funding.getId(), funding.getUserAccountId());
    }

    public static FundingContributionListResponse toContributionListResponse(
            Slice<FundingContribution> slice, Map<Long, User> userMap, int participantCount,
            Long totalAmount, int page, int size
    ) {
        List<FundingContributionListResponse.ContributionItem> items = slice.getContent().stream()
                .map(c -> toContributionItem(c, userMap))
                .toList();

        return new FundingContributionListResponse(
                participantCount, totalAmount, items, page, size, slice.hasNext()
        );
    }


    public static FundingVisibilityUpdateResponse toVisibilityUpdateResponse(FundingVisibilitySettings settings) {
        return new FundingVisibilityUpdateResponse(
                settings.getId(),
                settings.getIsProgressVisible(),
                settings.getIsCollectedAmountVisible(),
                settings.getIsParticipantCountVisible(),
                settings.getIsParticipantNameVisible(),
                settings.getIsMessageVisible()
        );
    }


    /**
     * 외부 방문자용 선물 준비 상세 응답 변환.
     *
     * [설계 포인트]
     *  - 공개 설정에 따른 null 마스킹을 이 한 곳에 모은다. 서비스에 if를 흩뿌리면
     *    "어떤 토글이 어떤 필드를 가리는지" 규칙이 흩어져 추적이 어려워진다.
     *  - visibility가 null이면 전체 공개로 간주한다. 공개 설정 행은 MY_GIFT 생성 시 항상 만들어지므로
     *    정상 흐름에선 비어 있을 수 없지만, 이상 데이터로 화면이 통째로 비는 것보다 안전하다.
     *    (축하 메시지 조회의 처리 방식과 동일한 규칙)
     *  - 마스킹 여부와 무관하게 visibility 플래그는 원본 그대로 내려간다.
     *    프론트가 "비공개"와 "값이 0/없음"을 구분해 안내 문구를 띄우는 데 쓴다.
     */
    public static SharedFundingDetailResponse toSharedFundingDetailResponse(
            Funding funding, Long collectedAmount, int participantCount,
            FundingVisibilitySettings visibility, List<FundingGift> gifts
    ) {
        boolean showProgress = isVisible(visibility == null ? null : visibility.getIsProgressVisible());
        boolean showAmount = isVisible(visibility == null ? null : visibility.getIsCollectedAmountVisible());
        boolean showParticipantCount = isVisible(visibility == null ? null : visibility.getIsParticipantCountVisible());
        boolean showParticipantNames = isVisible(visibility == null ? null : visibility.getIsParticipantNameVisible());
        boolean showMessages = isVisible(visibility == null ? null : visibility.getIsMessageVisible());

        return new SharedFundingDetailResponse(
                funding.getId(),
                funding.getTitle(),
                funding.getRecipientName(),
                funding.getAnniversaryDate(),
                funding.getStartDate(),
                funding.getEndDate(),
                funding.getIntroduction(),
                funding.getThumbnailImageUrl(),
                funding.getTargetAmount(),
                showAmount ? collectedAmount : null,
                showProgress ? calculateProgressRate(collectedAmount, funding.getTargetAmount()) : null,
                showParticipantCount ? participantCount : null,
                funding.getStatus().name(),
                new SharedFundingDetailResponse.VisibilityInfo(
                        showProgress, showAmount, showParticipantCount, showParticipantNames, showMessages
                ),
                gifts.stream()
                        .map(g -> new SharedFundingDetailResponse.GiftInfo(
                                g.getId(), g.getName(), g.getPrice(), g.getPurchaseUrl(), g.getImageUrl()
                        ))
                        .toList()
        );
    }

    /** 공개 설정값이 없으면(null) 공개로 간주한다 */
    private static boolean isVisible(Boolean flag) {
        return !Boolean.FALSE.equals(flag);
    }

    public static FundingMyGiftDashboardResponse toMyGiftDashboardResponse(
            Funding funding, Long collectedAmount, int participantCount,
            UserAccount account, FundingVisibilitySettings visibility, List<FundingGift> gifts
    ) {
        double progressRate = funding.getTargetAmount() == 0 ? 0.0
                : Math.round((collectedAmount * 10000.0 / funding.getTargetAmount())) / 100.0;

        return new FundingMyGiftDashboardResponse(
                funding.getId(), funding.getTitle(), funding.getRecipientName(),
                funding.getAnniversaryDate(), funding.getStartDate(), funding.getEndDate(),
                funding.getIntroduction(), funding.getThumbnailImageUrl(),
                funding.getTargetAmount(), collectedAmount, progressRate, participantCount,
                funding.getStatus().name(),
                account == null ? null : new FundingMyGiftDashboardResponse.AccountInfo(
                        account.getId(), account.getBankName().name(),
                        // 아이콘·표시명은 엔티티의 널가드 메서드를 거친다 — 백필 전 레거시 행은 bank가 null이다
                        account.getBankDisplayName(), account.getBankIconUrl(),
                        account.getAccount(), account.getAccountOwner()
                ),
                visibility == null ? null : new FundingMyGiftDashboardResponse.VisibilityInfo(
                        visibility.getIsProgressVisible(), visibility.getIsCollectedAmountVisible(),
                        visibility.getIsParticipantCountVisible(), visibility.getIsParticipantNameVisible(),
                        visibility.getIsMessageVisible()
                ),
                gifts.stream()
                        .map(g -> new FundingMyGiftDashboardResponse.GiftInfo(
                                g.getId(), g.getName(), g.getPrice(), g.getPurchaseUrl(), g.getImageUrl()
                        ))
                        .toList()
        );
    }

    public static FundingTogetherGiftDashboardResponse toTogetherGiftDashboardResponse(
            Funding funding, String myRole, List<FundingTogetherGiftDashboardResponse.MemberSummary> members,
            List<FundingTogetherGiftDashboardResponse.TopGift> topGifts,
            Long collectedAmount, Long targetAmount,
            List<FundingTogetherGiftDashboardResponse.ConfirmedGift> confirmedGifts,
            List<Long> messageIds
    ) {
        return new FundingTogetherGiftDashboardResponse(
                funding.getId(), funding.getStatus().name(), myRole, funding.getAnniversaryDate(),
                funding.getRecipientName(), funding.getIntroduction(), funding.getThumbnailImageUrl(),
                members, topGifts, collectedAmount, targetAmount, confirmedGifts, messageIds
        );
    }


    public static FundingContributionListResponse.ContributionItem toContributionItem(
            FundingContribution contribution, Map<Long, User> userMap
    ) {
        if (contribution.getUserId() != null) {
            User user = userMap.get(contribution.getUserId());
            String name = user != null ? user.getName() : null;
            String profileImageUrl = user != null ? user.getProfileImageUrl() : null;
            return new FundingContributionListResponse.ContributionItem(
                    contribution.getId(), name, profileImageUrl,
                    contribution.getAmount(), contribution.getCreatedAt()
            );
        }
        return new FundingContributionListResponse.ContributionItem(
                contribution.getId(), contribution.getGuestName(), null,
                contribution.getAmount(), contribution.getCreatedAt()
        );
    }

    public static FundingContributionListResponse toContributionListResponse(
            Slice<FundingContribution> slice, int participantCount, Long totalAmount,
            int page, int size, Map<Long, User> userMap
    ) {
        List<FundingContributionListResponse.ContributionItem> items = slice.getContent().stream()
                .map(c -> toContributionItem(c, userMap))
                .toList();

        return new FundingContributionListResponse(
                participantCount, totalAmount, items, page, size, slice.hasNext()

        );
    }

    public static FundingContributionAmountUpdateResponse toContributionAmountUpdateResponse(
            FundingContribution contribution
    ) {
        return new FundingContributionAmountUpdateResponse(
                contribution.getId(), contribution.getAmount()
        );
    }

}
