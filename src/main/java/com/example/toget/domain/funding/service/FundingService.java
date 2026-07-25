package com.example.toget.domain.funding.service;

import com.example.toget.domain.funding.dto.request.FundingCreateRequest;
import com.example.toget.domain.funding.dto.response.FundingCreateResponse;
import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.entity.FundingMember;
import com.example.toget.domain.funding.entity.FundingVisibilitySettings;
import com.example.toget.domain.funding.enums.FundingStatus;
import com.example.toget.domain.funding.enums.FundingType;
import com.example.toget.domain.funding.exception.FundingException;
import com.example.toget.domain.funding.exception.code.FundingErrorCode;
import com.example.toget.domain.funding.repository.FundingMemberRepository;
import com.example.toget.domain.funding.repository.FundingRepository;
import com.example.toget.domain.funding.repository.FundingVisibilitySettingsRepository;
import com.example.toget.domain.gift.entity.FundingGift;
import com.example.toget.domain.gift.repository.FundingGiftRepository;
import com.example.toget.domain.invitation.entity.CharacterEntity;
import com.example.toget.domain.invitation.entity.InvitationBackground;
import com.example.toget.domain.invitation.entity.InvitationCard;
import com.example.toget.domain.invitation.repository.CharacterRepository;
import com.example.toget.domain.invitation.repository.InvitationBackgroundRepository;
import com.example.toget.domain.invitation.repository.InvitationCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FundingService {

    private final FundingRepository fundingRepository;
    private final FundingMemberRepository fundingMemberRepository;
    private final FundingGiftRepository fundingGiftRepository;
    private final InvitationCardRepository invitationCardRepository;
    private final CharacterRepository characterRepository;
    private final InvitationBackgroundRepository invitationBackgroundRepository;
    private final FundingVisibilitySettingsRepository fundingVisibilitySettingsRepository;

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


        return new FundingCreateResponse(saved.getId());
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
}