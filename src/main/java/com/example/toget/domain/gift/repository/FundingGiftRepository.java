package com.example.toget.domain.gift.repository;

import com.example.toget.domain.funding.enums.FundingGiftStatus;
import com.example.toget.domain.gift.entity.FundingGift;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;

public interface FundingGiftRepository extends JpaRepository<FundingGift, Long> {

    List<FundingGift> findAllByFundingId(Long fundingId);

    List<FundingGift> findAllByFundingIdAndStatus(Long fundingId, FundingGiftStatus status);
}