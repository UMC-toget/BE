package com.example.toget.domain.funding.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "funding_review_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FundingReviewImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "funding_review_id", nullable = false)
    private Long fundingReviewId;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Builder
    private FundingReviewImage(Long fundingReviewId, String imageUrl) {
        this.fundingReviewId = fundingReviewId;
        this.imageUrl = imageUrl;
    }

    public static FundingReviewImage create(Long fundingReviewId, String imageUrl) {
        return FundingReviewImage.builder()
                .fundingReviewId(fundingReviewId)
                .imageUrl(imageUrl)
                .build();
    }
}