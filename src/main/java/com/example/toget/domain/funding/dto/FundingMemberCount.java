package com.example.toget.domain.funding.dto;

/**
 * 펀딩별 참여자(FundingMember) 수 집계 결과.
 * JPQL 생성자 표현식(select new ...)으로 GROUP BY 카운트 결과를 바로 담는다.
 */
public record FundingMemberCount(
        Long fundingId,
        Long memberCount
) {
}
