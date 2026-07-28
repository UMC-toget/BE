package com.example.toget.domain.funding.service;


import com.example.toget.domain.funding.dto.request.FundingReviewCreateRequest;
import com.example.toget.domain.funding.dto.request.FundingReviewTitledCreateRequest;
import com.example.toget.domain.funding.dto.response.FundingReviewCreateResponse;
import com.example.toget.domain.funding.dto.response.FundingReviewDetailResponse;
import com.example.toget.domain.funding.dto.response.FundingReviewInvitationResponse;
import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.enums.FundingType;
import com.example.toget.domain.funding.exception.ContributionException;
import com.example.toget.domain.funding.exception.FundingException;
import com.example.toget.domain.funding.exception.code.ContributionErrorCode;
import com.example.toget.domain.funding.exception.code.FundingErrorCode;
import com.example.toget.domain.funding.repository.ContributionBackgroundRepository;
import com.example.toget.domain.funding.repository.FundingRepository;
import com.example.toget.domain.funding.repository.FundingReviewImageRepository;
import com.example.toget.domain.funding.repository.FundingReviewRepository;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public FundingReviewCreateResponse createReview(
            Long userId, Long fundingId, FundingReviewCreateRequest request
    ) {
        Funding funding = validateOwnerAndType(userId, fundingId, FundingType.MY_GIFT);
        validateNotAlreadyExists(fundingId, FundingReviewType.REVIEW);

        contributionBackgroundRepository.findById(request.backgroundId())
                .orElseThrow(() -> new ContributionException(ContributionErrorCode.BACKGROUND_NOT_FOUND));

        FundingReview review = FundingReview.createReview(
                fundingId, request.content(), request.backgroundId(),
                request.invitationTitle(), request.invitationContent(),
                request.invitationCharacterId(), request.invitationBackgroundId()
        );
        FundingReview saved = fundingReviewRepository.save(review);
        saveImages(saved.getId(), request.images());

        return new FundingReviewCreateResponse(saved.getId());
    }

    @Transactional
    public FundingReviewCreateResponse createNews(
            Long userId, Long fundingId, FundingReviewTitledCreateRequest request
    ) {
        validateOwnerAndType(userId, fundingId, FundingType.TOGETHER_GIFT);
        validateNotAlreadyExists(fundingId, FundingReviewType.NEWS);

        FundingReview review = FundingReview.createNews(
                fundingId, request.title(), request.content(),
                request.invitationTitle(), request.invitationContent(),
                request.invitationCharacterId(), request.invitationBackgroundId()
        );
        FundingReview saved = fundingReviewRepository.save(review);
        saveImages(saved.getId(), request.images());

        return new FundingReviewCreateResponse(saved.getId());
    }

    @Transactional
    public FundingReviewCreateResponse createHeartfelt(
            Long userId, Long fundingId, FundingReviewTitledCreateRequest request
    ) {
        validateOwnerAndType(userId, fundingId, FundingType.TOGETHER_GIFT);
        validateNotAlreadyExists(fundingId, FundingReviewType.HEARTFELT);

        FundingReview review = FundingReview.createHeartfelt(
                fundingId, request.title(), request.content(),
                request.invitationTitle(), request.invitationContent(),
                request.invitationCharacterId(), request.invitationBackgroundId()
        );
        FundingReview saved = fundingReviewRepository.save(review);
        saveImages(saved.getId(), request.images());

        return new FundingReviewCreateResponse(saved.getId());
    }

    @Transactional(readOnly = true)
    public FundingReviewDetailResponse getReview(Long fundingId, String typeParam) {
        FundingReviewType type = parseType(typeParam);

        fundingRepository.findById(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));

        FundingReview review = fundingReviewRepository.findByFundingIdAndType(fundingId, type)
                .orElseThrow(() -> new FundingException(FundingErrorCode.REVIEW_NOT_FOUND));

        List<String> images = fundingReviewImageRepository.findAllByFundingReviewId(review.getId()).stream()
                .map(FundingReviewImage::getImageUrl)
                .toList();

        return FundingReviewConverter.toDetailResponse(review, images);
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