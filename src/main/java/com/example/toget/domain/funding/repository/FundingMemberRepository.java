package com.example.toget.domain.funding.repository;

import com.example.toget.domain.funding.entity.FundingMember;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FundingMemberRepository extends JpaRepository<FundingMember, Long> {

    List<FundingMember> findAllByFundingId(Long fundingId);
    List<FundingMember> findAllByFundingIdAndAmountDueIsNotNull(Long fundingId);

    Optional<FundingMember> findByFundingIdAndUserId(Long fundingId, Long userId);

    @Query("""
    SELECT m FROM FundingMember m
    WHERE m.fundingId = :fundingId
    ORDER BY
        CASE m.role
            WHEN com.example.toget.domain.funding.enums.FundingRole.CREATOR THEN 0
            WHEN com.example.toget.domain.funding.enums.FundingRole.ADMIN THEN 1
            ELSE 2
        END,
        m.createdAt ASC
    """)
    List<FundingMember> findTopMembersOrderByRole(@Param("fundingId") Long fundingId, Pageable pageable);
}