package com.example.toget.domain.funding.repository;

import com.example.toget.domain.funding.entity.FundingMember;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FundingMemberRepository extends JpaRepository<FundingMember, Long> {

    List<FundingMember> findAllByFundingId(Long fundingId);
    List<FundingMember> findAllByFundingIdAndAmountDueIsNotNull(Long fundingId);

    Optional<FundingMember> findByFundingIdAndUserId(Long fundingId, Long userId);

    /**
     * 입금 완료 신고처럼 "상태 확인 후 전이"가 한 트랜잭션에서 일어나는 호출 전용.
     * 동시 중복 요청(더블클릭, 재시도) 시 두 번째 트랜잭션이 첫 번째 커밋을 기다리게 해
     * settlementStatus 전이·후원 기록이 중복 저장되는 것을 막는다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM FundingMember m WHERE m.fundingId = :fundingId AND m.userId = :userId")
    Optional<FundingMember> findByFundingIdAndUserIdForUpdate(
            @Param("fundingId") Long fundingId, @Param("userId") Long userId);

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