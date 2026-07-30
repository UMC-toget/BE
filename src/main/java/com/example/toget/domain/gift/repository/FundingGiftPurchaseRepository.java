package com.example.toget.domain.gift.repository;

import com.example.toget.domain.gift.entity.FundingGiftPurchase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundingGiftPurchaseRepository extends JpaRepository<FundingGiftPurchase, Long> {

    boolean existsByFundingGiftId(Long fundingGiftId);
}