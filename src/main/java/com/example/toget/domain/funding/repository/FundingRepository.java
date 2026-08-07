package com.example.toget.domain.funding.repository;

import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.enums.FundingStatus;
import com.example.toget.domain.funding.enums.FundingType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * fundings 테이블 접근 리포지토리.
 */
public interface FundingRepository extends JpaRepository<Funding, Long> {

    // 단건 조회용 — soft delete된(deletedAt 기록된) 펀딩은 없는 것으로 취급한다 (deletedAt is null 컨벤션)
    Optional<Funding> findByIdAndDeletedAtIsNull(Long id);

    /**
     * 참여 종료일이 지났는데도 아직 정산 중(SETTLING)인 MY_GIFT 펀딩 자동 마감 배치 대상 조회.
     * - endDate < date: 종료일 당일까지는 유예하고, 다음날부터 마감 대상으로 잡는다.
     * - deletedAt is null: soft delete된 펀딩은 배치 대상에서 제외.
     */
    List<Funding> findAllByFundingTypeAndStatusAndEndDateBeforeAndDeletedAtIsNull(
            FundingType fundingType, FundingStatus status, LocalDate date);

    /**
     * 내가 개최한 펀딩 목록 페이징 조회.
     * - deletedAt is null: soft delete 대비 필터 (invitation 쪽과 동일한 규칙)
     * - order by createdAt desc: 등록일 최신 역순 (명세 고정 정렬이므로 쿼리에 명시)
     * - 반환형 Slice: 전체 개수(count 쿼리) 없이 다음 페이지 존재 여부만 필요해서 Page 대신 사용
     *
     * [추가사항]
     * Funding.userID는 개최자 FK. 타인 펀딩에 참여한 이력은 funding X / FundingMember FoundingContribution에 기록
     * 개최한 펀딩(MY_GIFT/TOGETHER_GIFT 전체)만 조회되고 참여 이력 안 섞임
     * 참여 이력 조회 FundingMember에 별도 쿼리 구현
     */
    @Query("select f from Funding f where f.userId = :userId and f.deletedAt is null order by f.createdAt desc")
    Slice<Funding> findMyHostedFundings(@Param("userId") Long userId, Pageable pageable);
}
