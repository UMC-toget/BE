package com.example.toget.domain.funding.service;

import com.example.toget.domain.funding.dto.FundingCollectedAmount;
import com.example.toget.domain.funding.dto.MyFundingListResponse;
import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.repository.FundingContributionRepository;
import com.example.toget.domain.funding.repository.FundingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * FundingQueryService 단위 테스트.
 * 리포지토리는 mock으로 대체하고 합산 매핑/페이징/보정 로직만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class FundingQueryServiceTest {

    @Mock
    private FundingRepository fundingRepository;

    @Mock
    private FundingContributionRepository fundingContributionRepository;

    @InjectMocks
    private FundingQueryService fundingQueryService;

    /** 테스트용 Funding 생성 — @GeneratedValue id는 영속화 전엔 null이라 리플렉션으로 주입 */
    private Funding fundingWithId(Long id) {
        Funding funding = Funding.createMyGift(1L, 100L, "길동이의 생일 펀딩", "홍길동",
                LocalDate.of(2026, 8, 14), LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 14), "소개글", "https://image.com/thumb.png", 1_000_000L);
        ReflectionTestUtils.setField(funding, "id", id);
        return funding;
    }

    @Test
    @DisplayName("참여금이 있는 펀딩은 합산 금액을, 없는 펀딩은 0을 collectedAmount로 매핑한다")
    void mapsCollectedAmountPerFunding() {
        Funding withMoney = fundingWithId(12L);
        Funding noMoney = fundingWithId(13L);
        given(fundingRepository.findMyFundings(eq(1L), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of(withMoney, noMoney), Pageable.ofSize(10), false));
        given(fundingContributionRepository.sumAmountsByFundingIds(anyList()))
                .willReturn(List.of(new FundingCollectedAmount(12L, 650_000L)));

        MyFundingListResponse response = fundingQueryService.getMyFundings(1L, 0, 10);

        assertThat(response.fundings()).hasSize(2);
        assertThat(response.fundings().get(0).collectedAmount()).isEqualTo(650_000L);
        assertThat(response.fundings().get(1).collectedAmount()).isZero();
        assertThat(response.fundings().get(0).status()).isEqualTo("SETTLING"); // MY_GIFT는 SETTLING으로 시작
    }

    @Test
    @DisplayName("Slice의 hasNext와 페이징 정보를 응답에 그대로 담는다")
    void mapsPagingInfo() {
        given(fundingRepository.findMyFundings(eq(1L), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of(fundingWithId(12L)),
                        org.springframework.data.domain.PageRequest.of(2, 5), true));
        given(fundingContributionRepository.sumAmountsByFundingIds(anyList()))
                .willReturn(List.of());

        MyFundingListResponse response = fundingQueryService.getMyFundings(1L, 2, 5);

        assertThat(response.currentPage()).isEqualTo(2);
        assertThat(response.pageSize()).isEqualTo(5);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    @DisplayName("개최한 펀딩이 없으면 빈 목록을 반환하고 합계 쿼리는 호출하지 않는다")
    void emptyResultSkipsSumQuery() {
        given(fundingRepository.findMyFundings(eq(1L), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of(), Pageable.ofSize(10), false));

        MyFundingListResponse response = fundingQueryService.getMyFundings(1L, 0, 10);

        assertThat(response.fundings()).isEmpty();
        assertThat(response.hasNext()).isFalse();
        verify(fundingContributionRepository, never()).sumAmountsByFundingIds(anyList());
    }

    @Test
    @DisplayName("음수 page는 0으로, 0 이하 size는 10으로 보정해 조회한다")
    void normalizesInvalidPaging() {
        given(fundingRepository.findMyFundings(eq(1L), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of(), Pageable.ofSize(10), false));

        fundingQueryService.getMyFundings(1L, -3, 0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(fundingRepository).findMyFundings(eq(1L), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
    }
}
