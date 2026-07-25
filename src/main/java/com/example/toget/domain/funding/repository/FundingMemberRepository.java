package com.example.toget.domain.funding.repository;

import com.example.toget.domain.funding.entity.FundingMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FundingMemberRepository extends JpaRepository<FundingMember, Long> {

    List<FundingMember> findAllByFundingId(Long fundingId);
    List<FundingMember> findAllByFundingIdAndAmountDueIsNotNull(Long fundingId);

    Optional<FundingMember> findByFundingIdAndUserId(Long fundingId, Long userId);
}