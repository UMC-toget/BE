package com.example.toget.domain.funding.dto;

/**
 * 펀딩별 참여금 합계 조회 결과.
 * JPQL 생성자 표현식(select new ...)으로 GROUP BY 합산 결과를 바로 담는다.
 */
public record FundingCollectedAmount(
        Long fundingId,
        Long collectedAmount
) {
}
