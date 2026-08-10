package com.example.toget.domain.funding.repository;

import com.example.toget.domain.funding.dto.FundingMemberCount;
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

    /**
     * 페이지에 담긴 펀딩들의 참여자 수를 fundingId별로 배치 집계한다 (N+1 방지).
     */
    @Query("""
    SELECT new com.example.toget.domain.funding.dto.FundingMemberCount(m.fundingId, COUNT(m))
    FROM FundingMember m
    WHERE m.fundingId IN :fundingIds
    GROUP BY m.fundingId
    """)
    List<FundingMemberCount> countMembersByFundingIds(@Param("fundingIds") List<Long> fundingIds);

    Optional<FundingMember> findByFundingIdAndUserId(Long fundingId, Long userId);

    /**
     * "상태 확인 후 전이/추가"가 한 트랜잭션에서 일어나는 호출 전용
     * (예: 입금 완료 신고 — settlementStatus 전이, 선물 후보 투표 토글 — 최대 투표 수 제한).
     * 동시 중복 요청(더블클릭, 재시도) 시 두 번째 트랜잭션이 첫 번째 커밋을 기다리게 해
     * 멤버 단위 불변 조건이 깨지는 것을 막는다.
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