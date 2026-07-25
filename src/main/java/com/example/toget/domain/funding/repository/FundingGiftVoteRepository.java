package com.example.toget.domain.funding.repository;

import com.example.toget.domain.gift.entity.FundingGiftVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FundingGiftVoteRepository extends JpaRepository<FundingGiftVote, Long> {

    Optional<FundingGiftVote> findByFundingMemberIdAndFundingGiftId(Long fundingMemberId, Long fundingGiftId);

    List<FundingGiftVote> findAllByFundingMemberId(Long fundingMemberId);

    /** 목록 조회용 배치 득표수 집계 — 후보 개수와 무관하게 쿼리 1번 */
    @Query("""
        SELECT v.fundingGiftId as giftId, COUNT(v) as voteCount
        FROM FundingGiftVote v
        WHERE v.fundingGiftId IN :giftIds
        GROUP BY v.fundingGiftId
        """)
    List<GiftVoteCountProjection> countVotesByGiftIds(@Param("giftIds") List<Long> giftIds);

    /** 상세 조회용 단건 득표수 */
    long countByFundingGiftId(Long fundingGiftId);

    interface GiftVoteCountProjection {
        Long getGiftId();
        Long getVoteCount();
    }
}