package com.example.toget.domain.funding.repository;

import com.example.toget.domain.funding.entity.FundingVisibilitySettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FundingVisibilitySettingsRepository extends JpaRepository<FundingVisibilitySettings, Long> {

    Optional<FundingVisibilitySettings> findByFundingId(Long fundingId);
}