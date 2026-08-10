package com.example.toget.domain.funding.service;

import com.example.toget.domain.funding.converter.FundingConverter;
import com.example.toget.domain.funding.dto.FundingCollectedAmount;
import com.example.toget.domain.funding.dto.MyFundingListResponse;
import com.example.toget.domain.funding.dto.response.FundingMyGiftDashboardResponse;
import com.example.toget.domain.funding.dto.response.FundingTogetherGiftDashboardResponse;
import com.example.toget.domain.funding.dto.response.SharedFundingDetailResponse;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final FundingReviewRepository fundingReviewRepository;


    /**
     * 내가 개최한 펀딩 목록 페이징 조회.
     * 쿼리는 페이지당 3번으로 고정: ① 펀딩 페이징 조회 ② 페이지 내 펀딩들의 참여금 IN 합산 ③ 후기 작성 여부 IN 조회.
     * (펀딩별로 합산하면 N+1이라 배치 쿼리로 묶는다)
     */
    public MyFundingListResponse getMyFundings(Long userId, int page, int size) {
        int safePage = Math.max(page, FIRST_PAGE);            // 음수 페이지 방어
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : size;  // 0 이하 크기 방어

        Slice<Funding> fundings = fundingRepository.findMyHostedFundings(userId, PageRequest.of(safePage, safeSize));
        List<Funding> content = fundings.getContent();
        return FundingConverter.toMyFundingListResponse(
                fundings,
                collectAmountsByFundingId(content),
                findReviewedFundingIds(content)
        );
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

    /**
     * 페이지에 담긴 펀딩들 중 후기가 작성된 fundingId Set 조회
     */
    private Set<Long> findReviewedFundingIds(List<Funding> fundings) {
        List<Long> fundingIds = fundings.stream().map(Funding::getId).toList();
        if (fundingIds.isEmpty()) {
            return Set.of();
        }
        List<Long> reviewedIds = fundingReviewRepository.findFundingIdsWithReviewIn(fundingIds);
        return reviewedIds != null ? new HashSet<>(reviewedIds) : Set.of();
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

        // 응답에 은행 아이콘이 실리므로 bank를 함께 읽는다(findWithBankById).
        // 일반 findById를 쓰면 LAZY인 bank 때문에 은행 SELECT가 한 번 더 나간다.
        UserAccount account = funding.getUserAccountId() != null
                ? userAccountRepository.findWithBankById(funding.getUserAccountId()).orElse(null)
                : null;

        FundingVisibilitySettings visibility = fundingVisibilitySettingsRepository
                .findByFundingId(fundingId).orElse(null);

        List<FundingGift> gifts = fundingGiftRepository.findAllByFundingId(fundingId);

        return FundingConverter.toMyGiftDashboardResponse(
                funding, collectedAmount, participantCount, account, visibility, gifts
        );
    }


    /**
     * 외부 방문자용 선물 준비 상세 조회 — 초대장 링크로 들어온 비회원에게 제공한다.
     *
     * [설계 포인트]
     *  - 개설자용 getMyGiftDashboard와 달리 소유자 검증(isOwnedBy)을 하지 않는다.
     *    누구나 볼 수 있는 공개 화면이라는 점이 이 API의 존재 이유다.
     *  - 대신 개설자가 지정한 공개 범위에 따라 값이 가려진다. 마스킹 판단은 컨버터에 위임한다.
     *  - MY_GIFT 전용이다. 함께 선물하기는 공개 설정 자체를 사용하지 않는다.
     *  - soft delete된 펀딩은 없는 것으로 취급한다. 외부에 공개되는 API라 삭제된 펀딩의 링크가 계속 유효하면 안 된다.
     */
    @Transactional(readOnly = true)
    public SharedFundingDetailResponse getSharedFundingDetail(Long fundingId) {
        Funding funding = fundingRepository.findByIdAndDeletedAtIsNull(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));
        if (funding.getFundingType() != FundingType.MY_GIFT) {
            throw new FundingException(FundingErrorCode.NOT_MY_GIFT_TYPE);
        }

        Long collectedAmount = fundingContributionRepository.sumAmountByFundingId(fundingId);
        int participantCount = fundingContributionRepository.countByFundingId(fundingId);

        FundingVisibilitySettings visibility = fundingVisibilitySettingsRepository
                .findByFundingId(fundingId).orElse(null);

        List<FundingGift> gifts = fundingGiftRepository.findAllByFundingId(fundingId);

        return FundingConverter.toSharedFundingDetailResponse(
                funding, collectedAmount, participantCount, visibility, gifts
        );
    }


    /**
     * TOGETHER_GIFT 상세 조회 — 개설자/공동관리자/일반참여자/비회원 모두 동일하게 조회 가능하다.
     *
     * [설계 포인트]
     *  - 초대장 링크를 아는 사람이면 누구나 볼 수 있는 화면이라는 점이 getSharedFundingDetail(MY_GIFT)과
     *    같은 결이다. TOGETHER_GIFT는 애초에 공개 범위(visibility) 개념이 없어 필드 마스킹도 하지 않는다.
     *  - 대신 응답에 myRole을 실어, 조회자가 이 펀딩에서 어떤 역할인지 프론트가 판단해 액션 버튼을
     *    켜고 끄도록 위임한다. 멤버가 아니거나(userId가 null이거나 join하지 않음) 비회원이면 null.
     *  - "조회는 열려 있다"는 것이지 "액션도 열려 있다"는 뜻이 아니다 — 투표/후기작성/멤버관리 등
     *    쓰기 API는 각자의 서비스 메서드에서 역할 검증을 그대로 유지한다.
     *  - soft delete된 펀딩은 없는 것으로 취급한다. 외부에 공개되는 API라 삭제된 펀딩의 링크가
     *    계속 유효하면 안 된다 (getSharedFundingDetail과 동일한 이유).
     */
    @Transactional(readOnly = true)
    public FundingTogetherGiftDashboardResponse getTogetherGiftDashboard(Long userId, Long fundingId) {
        Funding funding = fundingRepository.findByIdAndDeletedAtIsNull(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));
        if (funding.getFundingType() != FundingType.TOGETHER_GIFT) {
            throw new FundingException(FundingErrorCode.NOT_TOGETHER_GIFT_TYPE);
        }

        String myRole = resolveMyRole(userId, fundingId);

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
                funding, myRole, members, topGifts, collectedAmount, targetAmount, confirmedGifts, messageIds
        );
    }

    /** 비회원이거나(userId == null) 이 펀딩에 join하지 않은 회원이면 null */
    private String resolveMyRole(Long userId, Long fundingId) {
        if (userId == null) {
            return null;
        }
        return fundingMemberRepository.findByFundingIdAndUserId(fundingId, userId)
                .map(m -> m.getRole().name())
                .orElse(null);
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
