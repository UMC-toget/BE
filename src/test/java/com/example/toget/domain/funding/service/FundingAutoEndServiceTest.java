package com.example.toget.domain.funding.service;

import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.enums.FundingStatus;
import com.example.toget.domain.funding.enums.FundingType;
import com.example.toget.domain.funding.repository.FundingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * FundingAutoEndService(MY_GIFT 참여 종료일 경과 자동 마감 배치) 단위 테스트.
 * 크론 타이밍(FundingAutoEndScheduler)과 무관하게 배치 로직 자체만 검증한다.
 * 실행 시각에 따라 결과가 흔들리지 않도록 실제 시스템 시계 대신 고정된 Clock을 주입한다.
 */
@ExtendWith(MockitoExtension.class)
class FundingAutoEndServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 7);

    @Mock
    private FundingRepository fundingRepository;

    private FundingAutoEndService fundingAutoEndService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(TODAY.atStartOfDay(KST).toInstant(), KST);
        fundingAutoEndService = new FundingAutoEndService(fundingRepository, fixedClock);
    }

    @Test
    @DisplayName("종료일이 지난 SETTLING 상태 MY_GIFT 펀딩을 전부 ENDED로 전환한다")
    void autoEndsExpiredSettlingFundings() {
        // given
        Funding funding1 = Funding.createMyGift(1L, 1L, "제목1", "홍길동",
                TODAY, TODAY.minusDays(10), TODAY.minusDays(1),
                null, null, 100_000L);
        Funding funding2 = Funding.createMyGift(2L, 2L, "제목2", "김철수",
                TODAY, TODAY.minusDays(20), TODAY.minusDays(5),
                null, null, 50_000L);

        given(fundingRepository.findAllByFundingTypeAndStatusAndEndDateBeforeAndDeletedAtIsNull(
                FundingType.MY_GIFT, FundingStatus.SETTLING, TODAY))
                .willReturn(List.of(funding1, funding2));

        // when
        int result = fundingAutoEndService.autoEndExpiredMyGiftFundings();

        // then
        assertThat(result).isEqualTo(2);
        assertThat(funding1.getStatus()).isEqualTo(FundingStatus.ENDED);
        assertThat(funding2.getStatus()).isEqualTo(FundingStatus.ENDED);
    }

    @Test
    @DisplayName("대상 펀딩이 없으면 0을 반환하고 아무 것도 변경하지 않는다")
    void returnsZeroWhenNoTargets() {
        // given
        given(fundingRepository.findAllByFundingTypeAndStatusAndEndDateBeforeAndDeletedAtIsNull(
                FundingType.MY_GIFT, FundingStatus.SETTLING, TODAY))
                .willReturn(List.of());

        // when
        int result = fundingAutoEndService.autoEndExpiredMyGiftFundings();

        // then
        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("MY_GIFT + SETTLING + Clock 기준 오늘 날짜로만 조회한다 (TOGETHER_GIFT/다른 상태는 대상이 아님)")
    void queriesWithMyGiftSettlingAndToday() {
        // given
        given(fundingRepository.findAllByFundingTypeAndStatusAndEndDateBeforeAndDeletedAtIsNull(
                FundingType.MY_GIFT, FundingStatus.SETTLING, TODAY))
                .willReturn(List.of());

        ArgumentCaptor<FundingType> typeCaptor = ArgumentCaptor.forClass(FundingType.class);
        ArgumentCaptor<FundingStatus> statusCaptor = ArgumentCaptor.forClass(FundingStatus.class);
        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);

        // when
        fundingAutoEndService.autoEndExpiredMyGiftFundings();

        // then
        verify(fundingRepository).findAllByFundingTypeAndStatusAndEndDateBeforeAndDeletedAtIsNull(
                typeCaptor.capture(), statusCaptor.capture(), dateCaptor.capture());
        assertThat(typeCaptor.getValue()).isEqualTo(FundingType.MY_GIFT);
        assertThat(statusCaptor.getValue()).isEqualTo(FundingStatus.SETTLING);
        assertThat(dateCaptor.getValue()).isEqualTo(TODAY);
    }
}
