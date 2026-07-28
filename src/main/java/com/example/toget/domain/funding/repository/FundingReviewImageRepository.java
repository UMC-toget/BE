package com.example.toget.domain.funding.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FundingReviewImageRepository extends JpaRepository<FundingReviewImage, Long> {

    List<FundingReviewImage> findAllByFundingReviewId(Long fundingReviewId);
}