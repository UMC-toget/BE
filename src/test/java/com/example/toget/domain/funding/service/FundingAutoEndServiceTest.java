package com.example.toget.domain.funding.service;

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
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * FundingAutoEndService(MY_GIFT 참여 종료일 경과 자동 마감 배치) 단위 테스트.
 * 크론 타이밍(FundingAutoEndScheduler)과 무관하게 배치 로직 자체만 검증한다.
 * 실행 시각에 따라 결과가 흔들리지 않도록 실제 시스템 시계 대신 고정된 Clock을 주입한다.
 * <p>
 * 벌크 UPDATE(코드 리뷰 반영)로 바뀌면서 실제 엔티티 상태 변화는 레포지토리 계층 책임이라
 * 여기서는 "올바른 조건으로 벌크 UPDATE를 호출하는지"와 "영향받은 건수를 그대로 반환하는지"만 검증한다.
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
    @DisplayName("벌크 UPDATE가 처리한 건수를 그대로 반환한다")
    void returnsBulkUpdateCount() {
        // given
        given(fundingRepository.bulkEndExpiredFundings(
                eq(FundingType.MY_GIFT), eq(FundingStatus.SETTLING), eq(FundingStatus.ENDED),
                eq(TODAY), any(LocalDateTime.class)))
                .willReturn(2);

        // when
        int result = fundingAutoEndService.autoEndExpiredMyGiftFundings();

        // then
        assertThat(result).isEqualTo(2);
    }

    @Test
    @DisplayName("대상이 없으면 0을 반환한다")
    void returnsZeroWhenNoTargets() {
        // given
        given(fundingRepository.bulkEndExpiredFundings(
                eq(FundingType.MY_GIFT), eq(FundingStatus.SETTLING), eq(FundingStatus.ENDED),
                eq(TODAY), any(LocalDateTime.class)))
                .willReturn(0);

        // when
        int result = fundingAutoEndService.autoEndExpiredMyGiftFundings();

        // then
        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("MY_GIFT + SETTLING + ENDED + Clock 기준 오늘 날짜/시각으로 벌크 UPDATE를 호출한다")
    void callsBulkUpdateWithCorrectArgs() {
        // given
        given(fundingRepository.bulkEndExpiredFundings(
                eq(FundingType.MY_GIFT), eq(FundingStatus.SETTLING), eq(FundingStatus.ENDED),
                eq(TODAY), any(LocalDateTime.class)))
                .willReturn(0);

        ArgumentCaptor<FundingType> typeCaptor = ArgumentCaptor.forClass(FundingType.class);
        ArgumentCaptor<FundingStatus> currentStatusCaptor = ArgumentCaptor.forClass(FundingStatus.class);
        ArgumentCaptor<FundingStatus> newStatusCaptor = ArgumentCaptor.forClass(FundingStatus.class);
        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        // when
        fundingAutoEndService.autoEndExpiredMyGiftFundings();

        // then
        verify(fundingRepository).bulkEndExpiredFundings(
                typeCaptor.capture(), currentStatusCaptor.capture(), newStatusCaptor.capture(),
                dateCaptor.capture(), nowCaptor.capture());
        assertThat(typeCaptor.getValue()).isEqualTo(FundingType.MY_GIFT);
        assertThat(currentStatusCaptor.getValue()).isEqualTo(FundingStatus.SETTLING);
        assertThat(newStatusCaptor.getValue()).isEqualTo(FundingStatus.ENDED);
        assertThat(dateCaptor.getValue()).isEqualTo(TODAY);
        assertThat(nowCaptor.getValue()).isEqualTo(LocalDateTime.of(2026, 8, 7, 0, 0));
    }
}
