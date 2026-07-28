package com.example.toget.domain.funding.repository;


import com.example.toget.domain.funding.entity.FundingReview;
import com.example.toget.domain.funding.enums.FundingReviewType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FundingReviewRepository extends JpaRepository<FundingReview, Long> {

    Optional<FundingReview> findByFundingIdAndType(Long fundingId, FundingReviewType type);

    boolean existsByFundingIdAndType(Long fundingId, FundingReviewType type);
}