package com.example.toget.domain.funding.repository;

import com.example.toget.domain.funding.entity.FundingMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundingMemberRepository extends JpaRepository<FundingMember, Long> {
}