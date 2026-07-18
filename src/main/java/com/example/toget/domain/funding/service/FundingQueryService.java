package com.example.toget.domain.funding.service;

import com.example.toget.domain.funding.converter.FundingConverter;
import com.example.toget.domain.funding.dto.FundingCollectedAmount;
import com.example.toget.domain.funding.dto.MyFundingListResponse;
import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.repository.FundingContributionRepository;
import com.example.toget.domain.funding.repository.FundingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 펀딩 조회 전용 서비스.
 * 쓰기 로직과 분리해 조회만 담당하며, 전 메서드가 읽기 전용 트랜잭션으로 동작한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FundingQueryService {

    private static final int FIRST_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final FundingRepository fundingRepository;
    private final FundingContributionRepository fundingContributionRepository;

    /**
     * 내가 개최한 펀딩 목록 페이징 조회.
     * 쿼리는 페이지당 2번으로 고정: ① 펀딩 페이징 조회 ② 페이지 내 펀딩들의 참여금 IN 합산.
     * (펀딩별로 합산하면 N+1이라 배치 쿼리로 묶는다)
     */
    public MyFundingListResponse getMyFundings(Long userId, int page, int size) {
        int safePage = Math.max(page, FIRST_PAGE);            // 음수 페이지 방어
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : size;  // 0 이하 크기 방어

        Slice<Funding> fundings = fundingRepository.findMyFundings(userId, PageRequest.of(safePage, safeSize));
        return FundingConverter.toMyFundingListResponse(fundings, collectAmountsByFundingId(fundings.getContent()));
    }

    /** 페이지에 담긴 펀딩들의 참여금 합계를 fundingId → 합계 맵으로 조회 */
    private Map<Long, Long> collectAmountsByFundingId(List<Funding> fundings) {
        List<Long> fundingIds = fundings.stream().map(Funding::getId).toList();
        if (fundingIds.isEmpty()) {
            return Map.of(); // 빈 IN 절 쿼리 방지
        }
        return fundingContributionRepository.sumAmountsByFundingIds(fundingIds).stream()
                .collect(Collectors.toMap(FundingCollectedAmount::fundingId, FundingCollectedAmount::collectedAmount));
    }
}
