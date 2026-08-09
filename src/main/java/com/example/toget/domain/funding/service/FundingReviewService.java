package com.example.toget.domain.funding.service;


import com.example.toget.domain.funding.converter.FundingReviewConverter;
import com.example.toget.domain.funding.dto.request.FundingReviewCreateRequest;
import com.example.toget.domain.funding.dto.request.FundingReviewTitledCreateRequest;
import com.example.toget.domain.funding.dto.response.FundingReviewCreateResponse;
import com.example.toget.domain.funding.dto.response.FundingReviewDetailResponse;
import com.example.toget.domain.funding.dto.response.FundingReviewInvitationResponse;
import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.entity.FundingReview;
import com.example.toget.domain.funding.entity.FundingReviewImage;
import com.example.toget.domain.funding.enums.FundingReviewType;
import com.example.toget.domain.funding.enums.FundingStatus;
import com.example.toget.domain.funding.enums.FundingType;
import com.example.toget.domain.funding.exception.ContributionException;
import com.example.toget.domain.funding.exception.FundingException;
import com.example.toget.domain.funding.exception.code.ContributionErrorCode;
import com.example.toget.domain.funding.exception.code.FundingErrorCode;
import com.example.toget.domain.funding.repository.ContributionBackgroundRepository;
import com.example.toget.domain.funding.repository.FundingRepository;
import com.example.toget.domain.funding.repository.FundingReviewImageRepository;
import com.example.toget.domain.funding.repository.FundingReviewRepository;
import com.example.toget.domain.user.entity.User;
import com.example.toget.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FundingReviewService {

    private final FundingRepository fundingRepository;
    private final FundingReviewRepository fundingReviewRepository;
    private final FundingReviewImageRepository fundingReviewImageRepository;
    private final ContributionBackgroundRepository contributionBackgroundRepository;
    private final UserRepository userRepository;

    @Transactional
    public FundingReviewCreateResponse createReview(
            Long userId, Long fundingId, FundingReviewCreateRequest request
    ) {
        Funding funding = validateOwnerAndType(userId, fundingId, FundingType.MY_GIFT);
        requireEnded(funding);
        validateNotAlreadyExists(fundingId, FundingReviewType.REVIEW);

        contributionBackgroundRepository.findById(request.backgroundId())
                .orElseThrow(() -> new ContributionException(ContributionErrorCode.BACKGROUND_NOT_FOUND));

        FundingReview review = FundingReview.createReview(
                fundingId, request.content(), request.backgroundId(),
                request.invitationTitle(), request.invitationContent(),
                request.invitationCharacterId(), request.invitationBackgroundId()
        );
        FundingReview saved = saveReviewSafely(review);
        saveImages(saved.getId(), request.images());

        return new FundingReviewCreateResponse(saved.getId());
    }

    @Transactional
    public FundingReviewCreateResponse createNews(
            Long userId, Long fundingId, FundingReviewTitledCreateRequest request
    ) {
        Funding funding = validateOwnerAndType(userId, fundingId, FundingType.TOGETHER_GIFT);
        requireEnded(funding);
        validateNotAlreadyExists(fundingId, FundingReviewType.NEWS);

        FundingReview review = FundingReview.createNews(
                fundingId, request.title(), request.content(),
                request.invitationTitle(), request.invitationContent(),
                request.invitationCharacterId(), request.invitationBackgroundId()
        );
        FundingReview saved = saveReviewSafely(review);
        saveImages(saved.getId(), request.images());

        return new FundingReviewCreateResponse(saved.getId());
    }

    @Transactional
    public FundingReviewCreateResponse createHeartfelt(
            Long userId, Long fundingId, FundingReviewTitledCreateRequest request
    ) {
        Funding funding = validateOwnerAndType(userId, fundingId, FundingType.TOGETHER_GIFT);
        requireEnded(funding);
        validateNotAlreadyExists(fundingId, FundingReviewType.HEARTFELT);

        FundingReview review = FundingReview.createHeartfelt(
                fundingId, request.title(), request.content(),
                request.invitationTitle(), request.invitationContent(),
                request.invitationCharacterId(), request.invitationBackgroundId()
        );
        FundingReview saved = saveReviewSafely(review);
        saveImages(saved.getId(), request.images());


        return new FundingReviewCreateResponse(saved.getId());
    }

    private FundingReview saveReviewSafely(FundingReview review) {
        try {
            return fundingReviewRepository.save(review);
        } catch (DataIntegrityViolationException e) {
            throw new FundingException(FundingErrorCode.REVIEW_ALREADY_EXISTS);
        }
    }

    @Transactional(readOnly = true)
    public FundingReviewDetailResponse getReview(Long fundingId, String typeParam) {
        FundingReviewType type = parseType(typeParam);

        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));

        FundingReview review = fundingReviewRepository.findByFundingIdAndType(fundingId, type)
                .orElseThrow(() -> new FundingException(FundingErrorCode.REVIEW_NOT_FOUND));

        List<String> images = fundingReviewImageRepository.findAllByFundingReviewId(review.getId()).stream()
                .map(FundingReviewImage::getImageUrl)
                .toList();

        String authorName = resolveAuthorName(type, funding.getUserId());

        return FundingReviewConverter.toDetailResponse(review, images, authorName);
    }

    /**
     * 작성자 표시 이름 — REVIEW(선물 후기)/NEWS(전달 소식)만 채우고, HEARTFELT(마음전하기)는 null.
     *
     * REVIEW/NEWS는 개설자만 작성 가능하고(validateOwnerAndType), MY_GIFT는 funding_members를
     * 쓰지 않아 작성자가 항상 펀딩 개설자(Funding.userId) 한 명으로 고정된다. 그래서 후기마다
     * 작성자를 별도로 저장하지 않고 조회 시점에 조인해서 채운다.
     * 닉네임을 우선하고, 닉네임을 설정하지 않은 회원은 이름으로 대체한다(PM 확인 완료 — issue #105).
     */
    private String resolveAuthorName(FundingReviewType type, Long fundingOwnerUserId) {
        if (type == FundingReviewType.HEARTFELT) {
            return null;
        }
        return userRepository.findById(fundingOwnerUserId)
                .map(FundingReviewService::displayName)
                .orElse(null);
    }

    private static String displayName(User user) {
        return (user.getNickname() != null && !user.getNickname().isBlank())
                ? user.getNickname()
                : user.getName();
    }

    @Transactional(readOnly = true)
    public FundingReviewInvitationResponse getInvitation(Long fundingId, String typeParam) {
        FundingReviewType type = parseType(typeParam);

        fundingRepository.findById(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));

        FundingReview review = fundingReviewRepository.findByFundingIdAndType(fundingId, type)
                .orElseThrow(() -> new FundingException(FundingErrorCode.REVIEW_NOT_FOUND));

        return FundingReviewConverter.toInvitationResponse(review);
    }

    private Funding validateOwnerAndType(Long userId, Long fundingId, FundingType requiredType) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));
        if (!funding.isOwnedBy(userId)) {
            throw new FundingException(FundingErrorCode.NOT_FUNDING_OWNER);
        }
        if (funding.getFundingType() != requiredType) {
            throw new FundingException(
                    requiredType == FundingType.MY_GIFT
                            ? FundingErrorCode.NOT_MY_GIFT_TYPE
                            : FundingErrorCode.NOT_TOGETHER_GIFT_TYPE
            );
        }
        return funding;
    }

    /**
     * 후기(REVIEW)/전달 소식(NEWS)/마음전하기(HEARTFELT) 모두 펀딩이 종료(ENDED)된 뒤에만
     * 작성할 수 있다 — 정산·구매·전달이 끝나기도 전에 게시물부터 남기는 건 말이 안 되기 때문이다.
     * (PM 확인 완료 — issue #105)
     */
    private void requireEnded(Funding funding) {
        if (funding.getStatus() != FundingStatus.ENDED) {
            throw new FundingException(FundingErrorCode.INVALID_FUNDING_STATUS_FOR_REVIEW);
        }
    }

    private void validateNotAlreadyExists(Long fundingId, FundingReviewType type) {
        if (fundingReviewRepository.existsByFundingIdAndType(fundingId, type)) {
            throw new FundingException(FundingErrorCode.REVIEW_ALREADY_EXISTS);
        }
    }

    private void saveImages(Long fundingReviewId, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        List<FundingReviewImage> images = imageUrls.stream()
                .map(url -> FundingReviewImage.create(fundingReviewId, url))
                .toList();
        fundingReviewImageRepository.saveAll(images);
    }

    private FundingReviewType parseType(String typeParam) {
        try {
            return FundingReviewType.valueOf(typeParam.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new FundingException(FundingErrorCode.REVIEW_NOT_FOUND);
        }
    }
}