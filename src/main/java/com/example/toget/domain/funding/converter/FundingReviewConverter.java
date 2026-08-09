package com.example.toget.domain.funding.converter;

import com.example.toget.domain.funding.dto.response.FundingReviewDetailResponse;
import com.example.toget.domain.funding.dto.response.FundingReviewInvitationResponse;
import com.example.toget.domain.funding.entity.FundingReview;

import java.util.List;

public class FundingReviewConverter {

    private FundingReviewConverter() {
    }

    public static FundingReviewDetailResponse toDetailResponse(
            FundingReview review, List<String> images, String authorName
    ) {
        return new FundingReviewDetailResponse(
                review.getId(), review.getType().name(), authorName, review.getTitle(),
                review.getContent(), review.getBackgroundId(), images, review.getCreatedAt()
        );
    }

    public static FundingReviewInvitationResponse toInvitationResponse(FundingReview review) {
        return new FundingReviewInvitationResponse(
                review.getInvitationTitle(), review.getInvitationContent(),
                review.getInvitationCharacterId(), review.getInvitationBackgroundId()
        );
    }
}