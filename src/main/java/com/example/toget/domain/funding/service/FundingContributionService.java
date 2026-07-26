package com.example.toget.domain.funding.service;


import com.example.toget.domain.funding.converter.FundingContributionConverter;
import com.example.toget.domain.funding.dto.request.FundingContributionCreateRequest;
import com.example.toget.domain.funding.dto.response.FundingContributionCreateResponse;
import com.example.toget.domain.funding.dto.response.FundingContributionDetailResponse;
import com.example.toget.domain.funding.dto.response.FundingContributionRollingPaperResponse;
import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.entity.FundingContribution;
import com.example.toget.domain.funding.exception.ContributionException;
import com.example.toget.domain.funding.exception.FundingException;
import com.example.toget.domain.funding.exception.code.ContributionErrorCode;
import com.example.toget.domain.funding.exception.code.FundingErrorCode;
import com.example.toget.domain.funding.repository.ContributionBackgroundRepository;
import com.example.toget.domain.funding.repository.FundingContributionRepository;
import com.example.toget.domain.funding.repository.FundingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FundingContributionService {

    private final FundingRepository fundingRepository;
    private final FundingContributionRepository fundingContributionRepository;
    private final ContributionBackgroundRepository contributionBackgroundRepository;

    @Transactional
    public FundingContributionCreateResponse createContribution(
            Long userId, Long fundingId, FundingContributionCreateRequest request
    ) {
        fundingRepository.findById(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));

        contributionBackgroundRepository.findById(request.backgroundId())
                .orElseThrow(() -> new ContributionException(ContributionErrorCode.BACKGROUND_NOT_FOUND));

        boolean isMessageVisible = !request.isPrivate();

        FundingContribution contribution = (userId != null)
                ? FundingContribution.createForLoggedInUser(
                fundingId, request.backgroundId(), userId, request.isAnonymous(),
                request.amount(), request.content(), isMessageVisible)
                : FundingContribution.createForGuest(
                fundingId, request.backgroundId(), request.senderName(), request.isAnonymous(),
                request.amount(), request.content(), isMessageVisible);

        FundingContribution saved = fundingContributionRepository.save(contribution);

        return FundingContributionConverter.toCreateResponse(saved);
    }

    @Transactional(readOnly = true)
    public FundingContributionRollingPaperResponse getContributions(Long fundingId, Long viewerId) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));

        boolean isOwner = viewerId != null && funding.isOwnedBy(viewerId);

        List<FundingContribution> contributions = fundingContributionRepository.findAllByFundingId(fundingId);

        List<FundingContributionRollingPaperResponse.ContributionItem> items = contributions.stream()
                .map(c -> toListItem(c, isOwner))
                .toList();

        return new FundingContributionRollingPaperResponse(contributions.size(), items);
    }

    @Transactional(readOnly = true)
    public FundingContributionDetailResponse getContributionDetail(Long fundingId, Long contributionId, Long viewerId) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.FUNDING_NOT_FOUND));

        FundingContribution contribution = fundingContributionRepository.findById(contributionId)
                .orElseThrow(() -> new FundingException(FundingErrorCode.CONTRIBUTION_NOT_FOUND));
        if (!contribution.getFundingId().equals(fundingId)) {
            throw new FundingException(FundingErrorCode.CONTRIBUTION_NOT_FOUND);
        }

        boolean isOwner = viewerId != null && funding.isOwnedBy(viewerId);
        return FundingContributionConverter.toDetailResponse(contribution, isOwner);
    }

    private FundingContributionRollingPaperResponse.ContributionItem toListItem(FundingContribution c, boolean isOwner) {
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

    private boolean shouldHideContent(FundingContribution c, boolean isOwner) {
        return !isOwner && !c.getIsMessageVisible();
    }
}