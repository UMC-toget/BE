package com.example.toget.domain.funding.repository;

import com.example.toget.domain.funding.entity.FundingContribution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundingContributionRepository extends JpaRepository<FundingContribution, Long> {

    boolean existsByBackgroundId(Long backgroundId);
}