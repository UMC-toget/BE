package com.example.toget.domain.funding.service;

import com.example.toget.domain.funding.converter.FundingConverter;
import com.example.toget.domain.funding.dto.request.*;
import com.example.toget.domain.funding.dto.response.*;
import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.entity.FundingContribution;
import com.example.toget.domain.funding.entity.FundingMember;
import com.example.toget.domain.funding.entity.FundingVisibilitySettings;
import com.example.toget.domain.funding.enums.*;
import com.example.toget.domain
        .funding.exception.FundingException;
import com.example.toget.domain.funding.exception.ContributionException;
import com.example.toget.domain.funding.exception.code.ContributionErrorCode;
import com.example.toget.domain.funding.exception.code.FundingErrorCode;
import com.example.toget.domain.funding.repository.ContributionBackgroundRepository;
import com.example.toget.domain.funding.repository.FundingContributionRepository;
import com.example.toget.domain.funding.repository.FundingGiftVoteRepository;
import com.example.toget.domain.funding.repository.FundingMemberRepository;
import com.example.toget.domain.funding.repository.FundingRepository;
import com.example.toget.domain.funding.repository.FundingVisibilitySettingsRepository;
import com.example.toget.domain.gift.entity.FundingGift;
import com.example.toget.domain.gift.enums.FundingGiftStatus;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FundingService {

    private static final int MAX_SETTLEMENT_MEMBER_COUNT = 50;

    private final FundingRepository fundingRepository;
    private final FundingMemberRepository fundingMemberRepository;
    private final FundingGiftRepository fundingGiftRepository;
    private final FundingGiftVoteRepository fundingGiftVoteRepository;
    private final FundingContributionRepository fundingContributionRepository;
    private final ContributionBackgroundRepository contributionBackgroundRepository;
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
        // TOGETHER_GIFT는 targetAmount 미입력(null) 시 0으로 시작한다.
        Long targetAmount = request.targetAmount() != null ? request.targetAmount() : 0L;
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
                targetAmount
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

    /**
     * 펀딩 공개 설정 수정 — 외부 참여자/방문자에게 어떤 영역을 노출할지 제어한다.
     *
     * [설계 포인트]
     *  - MY_GIFT 전용. TOGETHER_GIFT는 생성 시점에 공개 설정 행을 만들지 않고(create() 참고),
     *    초대된 멤버끼리 서로의 참여·정산 현황을 모두 봐야 하는 구조라 가릴 대상이 없다.
     *  - 기본정보 수정(updateBasicInfo)과 달리 ENDED 상태에서도 허용한다.
     *    종료 후 "이제 참여자 이름은 가리고 싶다" 같은 요구가 자연스럽고,
     *    금액·기간과 달리 정산 정합성에 영향을 주지 않기 때문.
     */
    @Transactional
    public FundingVisibilityUpdateResponse updateVisibility(Long userId, Long fundingId,
                                                            FundingVisibilityUpdateRequest request) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));
        if (!funding.isOwnedBy(userId)) {
            throw new FundingException(FundingErrorCode.NOT_FUNDING_OWNER);
        }
        if (funding.getFundingType() != FundingType.MY_GIFT) {
            throw new FundingException(FundingErrorCode.NOT_MY_GIFT_TYPE);
        }

        // MY_GIFT는 생성 시 항상 함께 만들어지므로 정상 흐름에선 비어 있을 수 없지만,
        // 설정 행이 없는 기존 데이터를 대비해 없으면 이 시점에 빈 행을 만든다(upsert).
        // 값은 아래 update()가 채우므로 여기서는 요청값을 넘기지 않는다 — 순서 민감한 호출을 한 곳으로 모으기 위함.
        FundingVisibilitySettings settings = fundingVisibilitySettingsRepository.findByFundingId(fundingId)
                .orElseGet(() -> fundingVisibilitySettingsRepository.save(
                        FundingVisibilitySettings.createDefault(fundingId)
                ));

        // ⚠️ 인자 순서 주의 — 엔티티는 (진행률, 참여자 수, 참여자 이름, 메시지, 모금액) 순이라 모금액이 맨 뒤다.
        //    DTO/화면 순서(진행률, 모금액, ...)와 다르므로 임의로 재정렬하지 말 것.
        //    5개가 전부 Boolean이라 순서가 틀려도 컴파일 에러가 나지 않는다.
        //    (FundingServiceTest의 "각 토글 값이 대응하는 컬럼에 저장된다" 테스트가 이 매핑을 고정한다)
        settings.update(
                request.showProgress(),
                request.showParticipantCount(),
                request.showParticipantNames(),
                request.showMessages(),
                request.showAmount()
        );

        return FundingConverter.toVisibilityUpdateResponse(settings);
    }

    /**
     * 정산 계좌 조회 — 개설자뿐 아니라 비회원 참여자도 조회할 수 있다.
     *
     * [설계 포인트]
     *  - 계좌번호는 참여자가 실제로 입금할 대상이라, 로그인 여부와 무관하게 참여 흐름의 마지막
     *    단계에서 반드시 필요하다. shared-fundings/invitations와 같은 이유로 소유자 검증을 하지 않는다.
     *  - soft delete된 펀딩은 없는 것으로 취급한다. 외부에 공개되는 API라 삭제된 펀딩의 링크가
     *    계속 유효하면 안 된다 (getSharedFundingDetail과 동일한 이유).
     */
    @Transactional(readOnly = true)
    public FundingAccountResponse getAccount(Long fundingId) {
        Funding funding = fundingRepository.findByIdAndDeletedAtIsNull(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));
        if (funding.getUserAccountId() == null) {
            throw new FundingException(FundingErrorCode.ACCOUNT_NOT_REGISTERED);
        }

        // 응답에 은행 아이콘이 실리므로 bank를 함께 읽는다(findWithBankById)
        UserAccount account = userAccountRepository.findWithBankById(funding.getUserAccountId())
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

    /**
     * 초대장을 통해 들어온 회원이 함께 선물하기에 참여자(PARTICIPANT)로 합류.
     * 비회원은 FundingMember.userId가 not null이라 등록될 수 없으므로 로그인이 필수다.
     */
    @Transactional
    public FundingMemberJoinResponse join(Long userId, Long fundingId) {
        Funding funding = fundingRepository.findByIdAndDeletedAtIsNull(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));
        if (funding.getFundingType() != FundingType.TOGETHER_GIFT) {
            throw new FundingException(FundingErrorCode.NOT_TOGETHER_GIFT_TYPE);
        }
        if (fundingMemberRepository.findByFundingIdAndUserId(fundingId, userId).isPresent()) {
            throw new FundingException(FundingErrorCode.ALREADY_FUNDING_MEMBER);
        }

        FundingMember member = fundingMemberRepository.save(FundingMember.createParticipant(fundingId, userId));

        return new FundingMemberJoinResponse(funding.getId(), member.getId());
    }

    /**
     * 본인의 참여를 취소. SETTLING 이후에는 정산 대상자로 확정(amountDue 세팅)되었을 수 있어
     * 정산 인원·금액 정합성이 깨지므로 SELECTING 상태에서만 허용한다.
     * 남긴 투표 기록도 함께 삭제해 득표수 집계에 고아 데이터가 남지 않도록 한다.
     */
    @Transactional
    public void leave(Long userId, Long fundingId) {
        Funding funding = fundingRepository.findByIdAndDeletedAtIsNull(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));
        if (funding.getFundingType() != FundingType.TOGETHER_GIFT) {
            throw new FundingException(FundingErrorCode.NOT_TOGETHER_GIFT_TYPE);
        }

        FundingMember member = fundingMemberRepository.findByFundingIdAndUserId(fundingId, userId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.MEMBER_NOT_FOUND));

        if (member.getRole() == FundingRole.CREATOR) {
            throw new FundingException(FundingErrorCode.CREATOR_CANNOT_LEAVE_FUNDING);
        }
        if (funding.getStatus() != FundingStatus.SELECTING) {
            throw new FundingException(FundingErrorCode.INVALID_FUNDING_STATUS_FOR_LEAVE);
        }

        fundingGiftVoteRepository.deleteAllByFundingMemberId(member.getId());
        fundingMemberRepository.delete(member);
    }

    /**
     * 정산 대상으로 확정된 멤버 본인이 입금 완료를 신고 — 축하 메시지(FundingContribution)를
     * 함께 남기며 FundingMember.settlementStatus를 UNPAID → PAID로 전환한다.
     * amount는 정산 확정 시점에 이미 amountDue로 고정되어 있어 요청으로 받지 않고 그대로 기록하며,
     * 신원이 이미 확인된 로그인 멤버라 isAnonymous 없이 항상 실명으로 남긴다.
     */
    @Transactional
    public FundingSettlementContributionCreateResponse reportSettlementPayment(
            Long userId, Long fundingId, FundingSettlementContributionCreateRequest request
    ) {
        Funding funding = fundingRepository.findByIdAndDeletedAtIsNull(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));
        if (funding.getFundingType() != FundingType.TOGETHER_GIFT) {
            throw new FundingException(FundingErrorCode.NOT_TOGETHER_GIFT_TYPE);
        }

        FundingMember member = fundingMemberRepository.findByFundingIdAndUserIdForUpdate(fundingId, userId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.MEMBER_NOT_FOUND));

        contributionBackgroundRepository.findById(request.backgroundId())
                .orElseThrow(() -> new ContributionException(ContributionErrorCode.BACKGROUND_NOT_FOUND));

        // amountDue 미확정이면 NOT_SETTLEMENT_TARGET, 이미 UNPAID가 아니면 INVALID_SETTLEMENT_STATUS_TRANSITION
        member.requestPaymentConfirmation();

        boolean isMessageVisible = !request.isPrivate();
        FundingContribution contribution = FundingContribution.createForLoggedInUser(
                fundingId, request.backgroundId(), userId, false,
                member.getAmountDue(), request.content(), isMessageVisible
        );
        fundingContributionRepository.save(contribution);

        return new FundingSettlementContributionCreateResponse(
                funding.getId(), member.getId(), member.getSettlementStatus().name()
        );
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

        if (funding.getStatus() != FundingStatus.SELECTING) {
            throw new FundingException(FundingErrorCode.INVALID_FUNDING_STATUS_FOR_SETTLEMENT);
        }


        // 1단계: 존재/상태 검증만 먼저 전부 끝낸다 (아직 아무것도 mutate하지 않음)
        List<FundingGift> gifts = validateAndFetchGifts(fundingId, request.giftIds());
        List<Long> settlementMemberIds = includeCreator(fundingId, userId, request.settlementMemberIds());

        if (settlementMemberIds.size() > MAX_SETTLEMENT_MEMBER_COUNT) {
            throw new FundingException(FundingErrorCode.MAX_SETTLEMENT_MEMBER_EXCEEDED);
        }


        List<FundingMember> settlementMembers = validateAndFetchMembers(fundingId, settlementMemberIds);

        // 2단계: 검증이 전부 끝났으니 이제 안전하게 상태 변경
        long totalAmount = gifts.stream().mapToLong(FundingGift::getPrice).sum();
        gifts.forEach(FundingGift::select);
        assignSettlementAmounts(settlementMembers, totalAmount);
        funding.confirmSettlement(totalAmount);

        return new FundingConfirmSettlementResponse(funding.getId());
    }

    private List<FundingGift> validateAndFetchGifts(Long fundingId, List<Long> giftIds) {
        List<FundingGift> gifts = fundingGiftRepository.findAllById(giftIds);
        if (gifts.size() != giftIds.size()) {
            throw new FundingGiftException(FundingGiftErrorCode.FUNDING_GIFT_NOT_FOUND);
        }
        for (FundingGift gift : gifts) {
            if (!gift.getFundingId().equals(fundingId)) {
                throw new FundingGiftException(FundingGiftErrorCode.FUNDING_GIFT_NOT_FOUND);
            }
            if (gift.getStatus() != FundingGiftStatus.CANDIDATE) {
                throw new FundingGiftException(FundingGiftErrorCode.GIFT_ALREADY_SELECTED);
            }
        }
        return gifts;
    }


    /** 요청 배열에 개설자가 없으면 자동으로 추가. 이미 포함되어 있으면 중복 없이 그대로 사용 */
    private List<Long> includeCreator(Long fundingId, Long userId, List<Long> requestedMemberIds) {
        FundingMember creator = fundingMemberRepository.findByFundingIdAndUserId(fundingId, userId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.MEMBER_NOT_FOUND));

        if (requestedMemberIds.contains(creator.getId())) {
            return requestedMemberIds;
        }

        List<Long> merged = new ArrayList<>(requestedMemberIds);
        merged.add(creator.getId());
        return merged;
    }

    private List<FundingMember> validateAndFetchMembers(Long fundingId, List<Long> memberIds) {
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