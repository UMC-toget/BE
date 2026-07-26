package com.example.toget.domain.funding.service;

import com.example.toget.domain.funding.converter.FundingConverter;
import com.example.toget.domain.funding.dto.FundingCollectedAmount;
import com.example.toget.domain.funding.dto.MyFundingListResponse;
import com.example.toget.domain.funding.dto.response.FundingMyGiftDashboardResponse;
import com.example.toget.domain.funding.dto.response.FundingTogetherGiftDashboardResponse;
import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.entity.FundingMember;
import com.example.toget.domain.funding.entity.FundingVisibilitySettings;
import com.example.toget.domain.funding.enums.FundingStatus;
import com.example.toget.domain.funding.enums.FundingType;
import com.example.toget.domain.funding.exception.FundingException;
import com.example.toget.domain.funding.exception.code.FundingErrorCode;
import com.example.toget.domain.funding.repository.*;
import com.example.toget.domain.gift.entity.FundingGift;
import com.example.toget.domain.gift.enums.FundingGiftStatus;
import com.example.toget.domain.gift.repository.FundingGiftRepository;
import com.example.toget.domain.user.entity.User;
import com.example.toget.domain.user.entity.UserAccount;
import com.example.toget.domain.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 펀딩 조회 전용 서비스.
 * 쓰기 로직과 분리해 조회만 담당하며, 전 메서드가 읽기 전용 트랜잭션으로 동작한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FundingQueryService {

    private static final int FIRST_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MEMBER_SUMMARY_LIMIT = 3;
    private static final int TOP_GIFT_LIMIT = 2;
    private static final int RECENT_MESSAGE_LIMIT = 5;


    private final FundingRepository fundingRepository;
    private final FundingContributionRepository fundingContributionRepository;
    private final FundingMemberRepository fundingMemberRepository;
    private final FundingVisibilitySettingsRepository fundingVisibilitySettingsRepository;
    private final FundingGiftRepository fundingGiftRepository;
    private final FundingGiftVoteRepository fundingGiftVoteRepository;
    private final UserAccountRepository userAccountRepository;
    private final FundingMemberUserResolver fundingMemberUserResolver;


    /**
     * 내가 개최한 펀딩 목록 페이징 조회.
     * 쿼리는 페이지당 2번으로 고정: ① 펀딩 페이징 조회 ② 페이지 내 펀딩들의 참여금 IN 합산.
     * (펀딩별로 합산하면 N+1이라 배치 쿼리로 묶는다)
     */
    public MyFundingListResponse getMyFundings(Long userId, int page, int size) {
        int safePage = Math.max(page, FIRST_PAGE);            // 음수 페이지 방어
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : size;  // 0 이하 크기 방어

        Slice<Funding> fundings = fundingRepository.findMyHostedFundings(userId, PageRequest.of(safePage, safeSize));
        return FundingConverter.toMyFundingListResponse(fundings, collectAmountsByFundingId(fundings.getContent()));
    }

    /**
     * 페이지에 담긴 펀딩들의 참여금 합계를 fundingId → 합계 맵으로 조회
     */
    private Map<Long, Long> collectAmountsByFundingId(List<Funding> fundings) {
        List<Long> fundingIds = fundings.stream().map(Funding::getId).toList();
        if (fundingIds.isEmpty()) {
            return Map.of(); // 빈 IN 절 쿼리 방지
        }
        return fundingContributionRepository.sumAmountsByFundingIds(fundingIds).stream()
                .collect(Collectors.toMap(FundingCollectedAmount::fundingId, FundingCollectedAmount::collectedAmount));
    }

    @Transactional(readOnly = true)
    public FundingMyGiftDashboardResponse getMyGiftDashboard(Long userId, Long fundingId) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));
        if (!funding.isOwnedBy(userId)) {
            throw new FundingException(FundingErrorCode.NOT_FUNDING_OWNER);
        }
        if (funding.getFundingType() != FundingType.MY_GIFT) {
            throw new FundingException(FundingErrorCode.NOT_MY_GIFT_TYPE);
        }

        Long collectedAmount = fundingContributionRepository.sumAmountByFundingId(fundingId);
        int participantCount = fundingContributionRepository.countByFundingId(fundingId);

        UserAccount account = funding.getUserAccountId() != null
                ? userAccountRepository.findById(funding.getUserAccountId()).orElse(null)
                : null;

        FundingVisibilitySettings visibility = fundingVisibilitySettingsRepository
                .findByFundingId(fundingId).orElse(null);

        List<FundingGift> gifts = fundingGiftRepository.findAllByFundingId(fundingId);

        return FundingConverter.toMyGiftDashboardResponse(
                funding, collectedAmount, participantCount, account, visibility, gifts
        );
    }


    @Transactional(readOnly = true)
    public FundingTogetherGiftDashboardResponse getTogetherGiftDashboard(Long userId, Long fundingId) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));
        if (!funding.isOwnedBy(userId)) {
            throw new FundingException(FundingErrorCode.NOT_FUNDING_OWNER);
        }
        if (funding.getFundingType() != FundingType.TOGETHER_GIFT) {
            throw new FundingException(FundingErrorCode.NOT_TOGETHER_GIFT_TYPE);
        }

        List<FundingTogetherGiftDashboardResponse.MemberSummary> members = getMemberSummaries(fundingId);

        List<FundingTogetherGiftDashboardResponse.TopGift> topGifts = null;
        Long collectedAmount = null;
        Long targetAmount = null;
        List<FundingTogetherGiftDashboardResponse.ConfirmedGift> confirmedGifts = null;
        List<Long> messageIds = null;

        if (funding.getStatus() == FundingStatus.SELECTING) {
            topGifts = getTopVotedGifts(fundingId);
        } else {
            collectedAmount = fundingContributionRepository.sumAmountByFundingId(fundingId);
            targetAmount = funding.getTargetAmount();
            confirmedGifts = getConfirmedGifts(fundingId);

            if (funding.getStatus() == FundingStatus.ENDED) {
                messageIds = fundingContributionRepository
                        .findRecentIdsByFundingId(fundingId, PageRequest.of(0, RECENT_MESSAGE_LIMIT));
            }
        }

        return FundingConverter.toTogetherGiftDashboardResponse(
                funding, members, topGifts, collectedAmount, targetAmount, confirmedGifts, messageIds
        );
    }

    private List<FundingTogetherGiftDashboardResponse.MemberSummary> getMemberSummaries(Long fundingId) {
        List<FundingMember> members = fundingMemberRepository
                .findTopMembersOrderByRole(fundingId, PageRequest.of(0, MEMBER_SUMMARY_LIMIT));

        Map<Long, User> userMap = fundingMemberUserResolver.resolve(members);

        return members.stream()
                .map(m -> {
                    User user = userMap.get(m.getUserId());
                    return new FundingTogetherGiftDashboardResponse.MemberSummary(
                            m.getId(), m.getUserId(),
                            user != null ? user.getName() : null,
                            user != null ? user.getProfileImageUrl() : null,
                            m.getRole().name()
                    );
                })
                .toList();
    }

    private List<FundingTogetherGiftDashboardResponse.TopGift> getTopVotedGifts(Long fundingId) {
        List<FundingGift> candidates = fundingGiftRepository
                .findAllByFundingIdAndStatus(fundingId, FundingGiftStatus.CANDIDATE);
        List<Long> giftIds = candidates.stream().map(FundingGift::getId).toList();

        Map<Long, Long> voteCountMap = giftIds.isEmpty()
                ? Map.of()
                : fundingGiftVoteRepository.countVotesByGiftIds(giftIds).stream()
                .collect(Collectors.toMap(
                        FundingGiftVoteRepository.GiftVoteCountProjection::getGiftId,
                        FundingGiftVoteRepository.GiftVoteCountProjection::getVoteCount
                ));

        return candidates.stream()
                .map(g -> new FundingTogetherGiftDashboardResponse.TopGift(
                        g.getId(), g.getName(), g.getPrice(), g.getImageUrl(),
                        voteCountMap.getOrDefault(g.getId(), 0L)
                ))
                .sorted(Comparator.comparingLong(FundingTogetherGiftDashboardResponse.TopGift::voteCount).reversed())
                .limit(TOP_GIFT_LIMIT)
                .toList();
    }

    private List<FundingTogetherGiftDashboardResponse.ConfirmedGift> getConfirmedGifts(Long fundingId) {
        return fundingGiftRepository.findAllByFundingIdAndStatus(fundingId, FundingGiftStatus.SELECTED).stream()
                .map(g -> new FundingTogetherGiftDashboardResponse.ConfirmedGift(
                        g.getId(), g.getName(), g.getPrice(), g.getImageUrl()
                ))
                .toList();
    }


}
