package com.example.toget.domain.funding.repository;

import com.example.toget.domain.funding.dto.FundingCollectedAmount;
import com.example.toget.domain.funding.entity.FundingContribution;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * funding_contributions 테이블 접근 리포지토리.
 */
public interface FundingContributionRepository extends JpaRepository<FundingContribution, Long> {

    boolean existsByBackgroundId(Long backgroundId);

    /**
     * 여러 펀딩의 참여금 합계를 한 번에 조회 (목록 화면 N+1 방지용 배치 쿼리).
     * 참여금이 하나도 없는 펀딩은 결과에 아예 등장하지 않으므로,
     * 호출부에서 getOrDefault(fundingId, 0L)로 0 처리해야 한다.
     */
    @Query("""
            select new com.example.toget.domain.funding.dto.FundingCollectedAmount(fc.fundingId, sum(fc.amount))
            from FundingContribution fc
            where fc.fundingId in :fundingIds
            group by fc.fundingId
            """)
    List<FundingCollectedAmount> sumAmountsByFundingIds(@Param("fundingIds") List<Long> fundingIds);

    Slice<FundingContribution> findAllByFundingIdOrderByCreatedAtDesc(Long fundingId, Pageable pageable);
    Slice<FundingContribution> findAllByFundingIdOrderByCreatedAtAsc(Long fundingId, Pageable pageable);
    int countByFundingId(Long fundingId);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM FundingContribution c WHERE c.fundingId = :fundingId")
    Long sumAmountByFundingId(@Param("fundingId") Long fundingId);


    List<FundingContribution> findAllByFundingId(Long fundingId);

    @Query("SELECT c.id FROM FundingContribution c WHERE c.fundingId = :fundingId ORDER BY c.createdAt DESC")
    List<Long> findRecentIdsByFundingId(@Param("fundingId") Long fundingId, Pageable pageable);
}