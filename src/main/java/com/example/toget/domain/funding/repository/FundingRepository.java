package com.example.toget.domain.funding.repository;

import com.example.toget.domain.funding.entity.Funding;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * fundings 테이블 접근 리포지토리.
 */
public interface FundingRepository extends JpaRepository<Funding, Long> {

    /**
     * 내가 개최한 펀딩 목록 페이징 조회.
     * - deletedAt is null: soft delete 대비 필터 (invitation 쪽과 동일한 규칙)
     * - order by createdAt desc: 등록일 최신 역순 (명세 고정 정렬이므로 쿼리에 명시)
     * - 반환형 Slice: 전체 개수(count 쿼리) 없이 다음 페이지 존재 여부만 필요해서 Page 대신 사용
     */
    @Query("select f from Funding f where f.userId = :userId and f.deletedAt is null order by f.createdAt desc")
    Slice<Funding> findMyFundings(@Param("userId") Long userId, Pageable pageable);
}
