package com.example.toget.domain.funding.service;

import com.example.toget.domain.funding.converter.FundingConverter;
import com.example.toget.domain.funding.dto.request.*;
import com.example.toget.domain.funding.dto.response.*;
import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.entity.FundingContribution;
import com.example.toget.domain.funding.entity.FundingMember;
import com.example.toget.domain.funding.entity.FundingVisibilitySettings;
import com.example.toget.domain.funding.enums.*;
import com.example.toget.domain.funding.exception.FundingException;
import com.example.toget.domain.funding.exception.code.FundingErrorCode;
import com.example.toget.domain.funding.repository.FundingContributionRepository;
import com.example.toget.domain.funding.repository.FundingMemberRepository;
import com.example.toget.domain.funding.repository.FundingRepository;
import com.example.toget.domain.funding.repository.FundingVisibilitySettingsRepository;
import com.example.toget.domain.gift.entity.FundingGift;
import com.example.toget.domain.gift.exception.FundingGiftException;
import com.example.toget.domain.gift.exception.code.FundingGiftErrorCode;
import com.example.toget.domain.gift.repository.FundingGiftRepository;
import com.example.toget.domain.invitation.entity.CharacterEntity;
import com.example.toget.domain.invitation.entity.InvitationBackground;
import com.example.toget.domain.invitation.entity.InvitationCard;
import com.example.toget.domain.invitation.repository.CharacterRepository;
import com.example.toget.domain.invitation.repository.InvitationBackgroundRepository;
import com.example.toget.domain.invitation.repository.InvitationCardRepository;
import com.example.toget.domain.user.entity.User;
import com.example.toget.domain.user.entity.UserAccount;
import com.example.toget.domain.user.repository.UserAccountRepository;
import com.example.toget.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FundingService {


    private final FundingRepository fundingRepository;
    private final FundingMemberRepository fundingMemberRepository;
    private final FundingGiftRepository fundingGiftRepository;
    private final FundingContributionRepository fundingContributionRepository;
    private final InvitationCardRepository invitationCardRepository;
    private final CharacterRepository characterRepository;
    private final InvitationBackgroundRepository invitationBackgroundRepository;
    private final FundingVisibilitySettingsRepository fundingVisibilitySettingsRepository;
    private final UserRepository userRepository;
    private final UserAccountRepository userAccountRepository;
    private final FundingMemberUserResolver fundingMemberUserResolver;


    @Transactional
    public FundingCreateResponse create(Long userId, FundingCreateRequest request) {
        validateGifts(request);
        validateVisibility(request);

        Funding funding = switch (request.fundingType()) {
            case MY_GIFT -> createMyGift(userId, request);
            case TOGETHER_GIFT -> createTogetherGift(userId, request);
        };

        Funding saved = fundingRepository.save(funding);

        if (saved.getFundingType() == FundingType.TOGETHER_GIFT) {
            FundingMember creator = FundingMember.createCreator(saved.getId(), userId);
            fundingMemberRepository.save(creator);
        }

        if (saved.getFundingType() == FundingType.MY_GIFT) {
            saveVisibilitySettings(saved.getId(), request.visibility());
        }

        if (request.gifts() != null && !request.gifts().isEmpty()) {
            saveGifts(saved.getId(), request.gifts());
        }

        saveInvitationCard(saved.getId(), request.invitation());


        return FundingConverter.toCreateResponse(saved);
    }


    @Transactional
    public void updateStatus(Long userId, Long fundingId, FundingStatus newStatus) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));
        if (!funding.isOwnedBy(userId)) {
            throw new FundingException(FundingErrorCode.NOT_FUNDING_OWNER);
        }

        if (funding.getFundingType() == FundingType.MY_GIFT && newStatus != FundingStatus.ENDED) {
            throw new FundingException(FundingErrorCode.INVALID_FUNDING_STATUS_TRANSITION);
        }

        // [TOGETHER_GIFT] SELECTING → SETTLING → PURCHASING → DELIVERING → ENDED 순서로 전환합니다.
        //  단, SELECTING → SETTLING 전환은 이 API가 아니라 POST /fundings/{fundingId}/confirm-settlement(선물 확정하기) API를 통해서만 가능합니다.
        //  이 API는 PURCHASING, DELIVERING, ENDED로의 전환만 처리합니다.
        // [MY_GIFT] SETTLING → ENDED 전환만 가능합니다.
        switch (newStatus) {
            case PURCHASING -> funding.startPurchasing();
            case DELIVERING -> funding.startDelivering();
            case ENDED -> funding.complete();
            default -> throw new FundingException(FundingErrorCode.INVALID_FUNDING_STATUS_TRANSITION);
        }
    }



    private void validateGifts(FundingCreateRequest request) {
        boolean giftsEmpty = request.gifts() == null || request.gifts().isEmpty();
        if (request.fundingType() == FundingType.MY_GIFT && giftsEmpty) {
            throw new FundingException(FundingErrorCode.GIFT_REQUIRED_FOR_MY_GIFT);
        }
    }

    private void validateVisibility(FundingCreateRequest request) {
        if (request.fundingType() == FundingType.MY_GIFT && request.visibility() == null) {
            throw new FundingException(FundingErrorCode.VISIBILITY_REQUIRED_FOR_MY_GIFT);
        }
    }

    private void saveVisibilitySettings(Long fundingId, FundingCreateRequest.VisibilityRequest v) {
        FundingVisibilitySettings settings = FundingVisibilitySettings.create(
                fundingId,
                v.showProgress(),
                v.showParticipantCount(),
                v.showParticipantNames(),
                v.showMessages(),
                v.showAmount()
        );
        fundingVisibilitySettingsRepository.save(settings);
    }


    private void saveGifts(Long fundingId, List<FundingCreateRequest.GiftRequest> giftRequests) {
        List<FundingGift> gifts = giftRequests.stream()
                .map(g -> FundingGift.builder()
                        .fundingId(fundingId)
                        .name(g.giftName())
                        .price(g.giftPrice())
                        .purchaseUrl(g.giftPurchaseUrl())
                        .imageUrl(g.giftImageUrl())
                        .note("")  // note 필드 처리 방식 확인 필요 — 임시로 빈 문자열
                        .build())
                .toList();
        fundingGiftRepository.saveAll(gifts);
    }

    private void saveInvitationCard(Long fundingId, FundingCreateRequest.InvitationRequest request) {
        CharacterEntity character = characterRepository.findById(request.characterId())
                .orElseThrow(() -> new FundingException(FundingErrorCode.CHARACTER_NOT_FOUND));
        InvitationBackground background = invitationBackgroundRepository.findById(request.backgroundId())
                .orElseThrow(() -> new FundingException(FundingErrorCode.INVITATION_BACKGROUND_NOT_FOUND));

        // TODO: URL 생성 정책 확정 전까지의 임시 방식
        String url = "https://toget.com/funding/" + fundingId + "/invitation";

        InvitationCard card = InvitationCard.builder()
                .fundingId(fundingId)
                .character(character)
                .background(background)
                .title(request.title())
                .content(request.content())
                .url(url)
                .build();

        invitationCardRepository.save(card);
    }

    private Funding createMyGift(Long userId, FundingCreateRequest request) {
        return Funding.createMyGift(
                userId,
                request.userAccountId(),
                request.title(),
                request.recipientName(),
                request.anniversaryDate(),
                request.startDate(),
                request.endDate(),
                request.introduction(),
                request.thumbnailImageUrl(),
                request.targetAmount()
        );
    }

    private Funding createTogetherGift(Long userId, FundingCreateRequest request) {
        return Funding.createTogetherGift(
                userId,
                request.userAccountId(),
                request.title(),
                request.recipientName(),
                request.anniversaryDate(),
                request.startDate(),
                request.endDate(),
                request.introduction(),
                request.thumbnailImageUrl(),
                request.targetAmount()
        );
    }

    @Transactional
    public FundingBasicInfoResponse updateBasicInfo(Long userId, Long fundingId, FundingBasicInfoUpdateRequest request) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));
        if (!funding.isOwnedBy(userId)) {
            throw new FundingException(FundingErrorCode.NOT_FUNDING_OWNER);
        }

        if (funding.getStatus() == FundingStatus.ENDED) {
            throw new FundingException(FundingErrorCode.FUNDING_ALREADY_ENDED);
        }


        boolean periodChanged = !request.startDate().equals(funding.getStartDate())
                || !request.endDate().equals(funding.getEndDate());
        if (periodChanged) {
            if (!isPeriodEditable(funding.getStatus())) {
                throw new FundingException(FundingErrorCode.INVALID_STATUS_FOR_PERIOD_UPDATE);
            }
            if (!request.endDate().isAfter(LocalDate.now())) {
                throw new FundingException(FundingErrorCode.END_DATE_MUST_BE_FUTURE);
            }
        }


        funding.updateBasicInfo(
                request.title(), request.anniversaryDate(), request.startDate(),
                request.endDate(), request.introduction(), request.thumbnailImageUrl()
        );

        return FundingConverter.toBasicInfoResponse(funding);
    }

    private boolean isPeriodEditable(FundingStatus status) {
        return status == FundingStatus.SELECTING || status == FundingStatus.SETTLING;
    }

    @Transactional(readOnly = true)
    public FundingAccountResponse getAccount(Long userId, Long fundingId) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));
        if (!funding.isOwnedBy(userId)) {
            throw new FundingException(FundingErrorCode.NOT_FUNDING_OWNER);
        }
        if (funding.getUserAccountId() == null) {
            throw new FundingException(FundingErrorCode.ACCOUNT_NOT_REGISTERED);
        }

        UserAccount account = userAccountRepository.findById(funding.getUserAccountId())
                .orElseThrow(() -> new FundingException(FundingErrorCode.ACCOUNT_NOT_FOUND));

        return FundingConverter.toAccountResponse(account);
    }

    @Transactional
    public FundingAccountUpdateResponse updateAccount(Long userId, Long fundingId, FundingAccountUpdateRequest request) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));
        if (!funding.isOwnedBy(userId)) {
            throw new FundingException(FundingErrorCode.NOT_FUNDING_OWNER);
        }

        UserAccount account = userAccountRepository.findById(request.userAccountId())
                .orElseThrow(() -> new FundingException(FundingErrorCode.ACCOUNT_NOT_FOUND));
        if (!account.getUserId().equals(userId)) {
            throw new FundingException(FundingErrorCode.NOT_ACCOUNT_OWNER);
        }

        funding.updateAccount(request.userAccountId());

        return FundingConverter.toAccountUpdateResponse(funding);
    }

    @Transactional(readOnly = true)
    public FundingMemberManagementResponse getMembers(Long userId, Long fundingId) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));
        if (!funding.isOwnedBy(userId)) {
            throw new FundingException(FundingErrorCode.NOT_FUNDING_OWNER);
        }
        if (funding.getStatus() != FundingStatus.SELECTING) {
            throw new FundingException(FundingErrorCode.DASHBOARD_STATUS_MISMATCH);
        }

        List<FundingMember> members = fundingMemberRepository.findAllByFundingId(fundingId);
        Map<Long, User> userMap = fundingMemberUserResolver.resolve(members);

        return FundingConverter.toMemberManagementResponse(members, userMap);
    }

    @Transactional
    public void updateMemberRole(Long userId, Long fundingId, Long memberId, FundingRole newRole) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));
        if (!funding.isOwnedBy(userId)) {
            throw new FundingException(FundingErrorCode.NOT_FUNDING_OWNER);
        }

        FundingMember member = fundingMemberRepository.findById(memberId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.MEMBER_NOT_FOUND));
        if (!member.getFundingId().equals(fundingId)) {
            throw new FundingException(FundingErrorCode.MEMBER_NOT_FOUND);
        }

        if (newRole == FundingRole.ADMIN) {
            member.promoteToAdmin();
        } else if (newRole == FundingRole.PARTICIPANT) {
            member.demoteToParticipant();
        } else {
            throw new FundingException(FundingErrorCode.INVALID_MEMBER_ROLE);
        }
    }

    @Transactional(readOnly = true)
    public FundingSettlementListResponse getSettlements(Long userId, Long fundingId) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));
        if (!funding.isOwnedBy(userId)) {
            throw new FundingException(FundingErrorCode.NOT_FUNDING_OWNER);
        }
        if (funding.getStatus() == FundingStatus.SELECTING) {
            throw new FundingException(FundingErrorCode.DASHBOARD_STATUS_MISMATCH);
        }

        List<FundingMember> settlementMembers = fundingMemberRepository
                .findAllByFundingIdAndAmountDueIsNotNull(fundingId);
        Map<Long, User> userMap = fundingMemberUserResolver.resolve(settlementMembers);

        long totalAmount = settlementMembers.stream()
                .mapToLong(FundingMember::getAmountDue)
                .sum();

        List<FundingSettlementListResponse.SettlementInfo> settlements = settlementMembers.stream()
                .map(m -> new FundingSettlementListResponse.SettlementInfo(
                        m.getId(), m.getUserId(),
                        userMap.get(m.getUserId()).getName(),
                        userMap.get(m.getUserId()).getProfileImageUrl(),
                        m.getAmountDue(), m.getSettlementStatus().name()
                ))
                .toList();

        return FundingConverter.toSettlementListResponse(settlementMembers, userMap);
    }

    @Transactional
    public void updateSettlementStatus(Long userId, Long fundingId, Long memberId, SettlementStatus newStatus) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));
        if (!funding.isOwnedBy(userId)) {
            throw new FundingException(FundingErrorCode.NOT_FUNDING_OWNER);
        }

        FundingMember member = fundingMemberRepository.findById(memberId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.MEMBER_NOT_FOUND));
        if (!member.getFundingId().equals(fundingId)) {
            throw new FundingException(FundingErrorCode.MEMBER_NOT_FOUND);
        }

        if (!member.isSettlementTarget()) {
            throw new FundingException(FundingErrorCode.NOT_SETTLEMENT_TARGET_MEMBER);
        }

        switch (newStatus) {
            case CONFIRMED -> member.confirmPayment();              // PAID → CONFIRMED
            case PAID -> member.revertPaymentConfirmation();        // CONFIRMED → PAID
            case UNPAID -> member.revertToUnpaid();                 // PAID → UNPAID (CONFIRMED에선 불가)
            default -> throw new FundingException(FundingErrorCode.INVALID_SETTLEMENT_STATUS_TRANSITION);
        }
    }



    @Transactional(readOnly = true)
    public FundingContributionListResponse getContributions(
            Long userId, Long fundingId, ContributionSortType sort, int page, int size
    ) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));
        if (!funding.isOwnedBy(userId)) {
            throw new FundingException(FundingErrorCode.NOT_FUNDING_OWNER);
        }
        if (funding.getFundingType() != FundingType.MY_GIFT) {
            throw new FundingException(FundingErrorCode.NOT_MY_GIFT_TYPE);
        }

        Pageable pageable = PageRequest.of(page, size);
        Slice<FundingContribution> slice = (sort == ContributionSortType.OLDEST)
                ? fundingContributionRepository.findAllByFundingIdOrderByCreatedAtAsc(fundingId, pageable)
                : fundingContributionRepository.findAllByFundingIdOrderByCreatedAtDesc(fundingId, pageable);

        int participantCount = fundingContributionRepository.countByFundingId(fundingId);
        Long totalAmount = fundingContributionRepository.sumAmountByFundingId(fundingId);

        Map<Long, User> userMap = getUserMapFromContributions(slice.getContent());

        return FundingConverter.toContributionListResponse(
                slice, participantCount, totalAmount, page, size, userMap
        );
    }

    /** 로그인 회원 참여자들의 User 정보를 배치 조회, 비회원은 필터링 (N+1 방지) */
    private Map<Long, User> getUserMapFromContributions(List<FundingContribution> contributions) {
        List<Long> userIds = contributions.stream()
                .map(FundingContribution::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private FundingContributionListResponse.ContributionItem toContributionItem(
            FundingContribution c, Map<Long, User> userMap
    ) {
        if (c.getUserId() != null) {
            // 로그인 회원 참여 — User 조회로 이름/프로필 채움
            User user = userMap.get(c.getUserId());
            String name = user != null ? user.getName() : null;
            String profileImageUrl = user != null ? user.getProfileImageUrl() : null;
            return new FundingContributionListResponse.ContributionItem(
                    c.getId(), name, profileImageUrl, c.getAmount(), c.getCreatedAt()
            );
        }
        // 비회원 참여 — guestName 그대로, 프로필 이미지는 애초에 없음
        return new FundingContributionListResponse.ContributionItem(
                c.getId(), c.getGuestName(), null, c.getAmount(), c.getCreatedAt()
        );
    }

    @Transactional
    public FundingContributionAmountUpdateResponse updateContributionAmount(
            Long userId, Long fundingId, Long contributionId, FundingContributionAmountUpdateRequest request
    ) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));
        if (!funding.isOwnedBy(userId)) {
            throw new FundingException(FundingErrorCode.NOT_FUNDING_OWNER);
        }
        if (funding.getFundingType() != FundingType.MY_GIFT) {
            throw new FundingException(FundingErrorCode.NOT_MY_GIFT_TYPE);
        }

        FundingContribution contribution = fundingContributionRepository.findById(contributionId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.CONTRIBUTION_NOT_FOUND));
        if (!contribution.getFundingId().equals(fundingId)) {
            throw new FundingException(FundingErrorCode.CONTRIBUTION_NOT_FOUND);
        }

        contribution.updateAmount(request.amount());

        return FundingConverter.toContributionAmountUpdateResponse(contribution);
    }

    @Transactional
    public FundingConfirmSettlementResponse confirmSettlement(
            Long userId, Long fundingId, FundingConfirmSettlementRequest request
    ) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));
        if (!funding.isOwnedBy(userId)) {
            throw new FundingException(FundingErrorCode.NOT_FUNDING_OWNER);
        }
        if (funding.getFundingType() != FundingType.TOGETHER_GIFT) {
            throw new FundingException(FundingErrorCode.NOT_TOGETHER_GIFT_TYPE);
        }

        List<FundingGift> selectedGifts = confirmGifts(fundingId, request.giftIds());
        long totalAmount = selectedGifts.stream().mapToLong(FundingGift::getPrice).sum();

        List<FundingMember> settlementMembers = confirmSettlementMembers(fundingId, request.settlementMemberIds());
        assignSettlementAmounts(settlementMembers, totalAmount);

        funding.confirmSettlement(totalAmount);

        return new FundingConfirmSettlementResponse(funding.getId());
    }

    private List<FundingGift> confirmGifts(Long fundingId, List<Long> giftIds) {
        List<FundingGift> gifts = fundingGiftRepository.findAllById(giftIds);
        if (gifts.size() != giftIds.size()) {
            throw new FundingGiftException(FundingGiftErrorCode.FUNDING_GIFT_NOT_FOUND);
        }
        for (FundingGift gift : gifts) {
            if (!gift.getFundingId().equals(fundingId)) {
                throw new FundingGiftException(FundingGiftErrorCode.FUNDING_GIFT_NOT_FOUND);
            }
            gift.select();
        }
        return gifts;
    }

    private List<FundingMember> confirmSettlementMembers(Long fundingId, List<Long> memberIds) {
        List<FundingMember> members = fundingMemberRepository.findAllById(memberIds);
        if (members.size() != memberIds.size()) {
            throw new FundingException(FundingErrorCode.MEMBER_NOT_FOUND);
        }
        for (FundingMember member : members) {
            if (!member.getFundingId().equals(fundingId)) {
                throw new FundingException(FundingErrorCode.MEMBER_NOT_FOUND);
            }
        }
        return members;
    }

    private void assignSettlementAmounts(List<FundingMember> members, long totalAmount) {
        List<FundingMember> sorted = members.stream()
                .sorted(Comparator.comparing(FundingMember::getCreatedAt))
                .toList();

        long base = totalAmount / sorted.size();
        long remainder = totalAmount % sorted.size();

        for (int i = 0; i < sorted.size(); i++) {
            long amount = base + (i < remainder ? 1 : 0);
            sorted.get(i).confirmSettlement(amount);
        }
    }
}