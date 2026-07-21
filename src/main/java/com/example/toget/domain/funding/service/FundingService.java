package com.example.toget.domain.funding.service;

import com.example.toget.domain.funding.dto.request.FundingCreateRequest;
import com.example.toget.domain.funding.dto.response.FundingCreateResponse;
import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.entity.FundingMember;
import com.example.toget.domain.funding.enums.FundingType;
import com.example.toget.domain.funding.repository.FundingMemberRepository;
import com.example.toget.domain.funding.repository.FundingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FundingService {

    private final FundingRepository fundingRepository;
    private final FundingMemberRepository fundingMemberRepository;

    @Transactional
    public FundingCreateResponse create(Long userId, FundingCreateRequest request) {
        Funding funding = switch (request.fundingType()) {
            case MY_GIFT -> createMyGift(userId, request);
            case TOGETHER_GIFT -> createTogetherGift(userId, request);
        };

        Funding saved = fundingRepository.save(funding);

        if (saved.getFundingType() == FundingType.TOGETHER_GIFT) {
            FundingMember creator = FundingMember.createCreator(saved.getId(), userId);
            fundingMemberRepository.save(creator);
        }

        return new FundingCreateResponse(saved.getId());
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