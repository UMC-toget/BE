package com.example.toget.domain.funding.service;

import com.example.toget.domain.funding.dto.request.FundingContributionAmountUpdateRequest;
import com.example.toget.domain.funding.dto.response.FundingContributionAmountUpdateResponse;
import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.entity.FundingContribution;
import com.example.toget.domain.funding.exception.FundingException;
import com.example.toget.domain.funding.exception.code.FundingErrorCode;
import com.example.toget.domain.funding.repository.FundingContributionRepository;
import com.example.toget.domain.funding.repository.FundingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * FundingService(쓰기 로직) 단위 테스트.
 * 기본정보/기여금액 수정 등 write 트랜잭션을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class FundingServiceTest {

    @Mock
    private FundingRepository fundingRepository;

    @Mock
    private FundingContributionRepository fundingContributionRepository;

    @InjectMocks
    private FundingService fundingService;

    @Nested
    @DisplayName("기본정보 수정")
    class UpdateBasicInfo {

        @Test
        @DisplayName("유효한 값으로 수정하면 필드가 갱신된다")
        void updateBasicInfo_success() {
            Funding funding = Funding.createMyGift(1L, 1L, "제목", "홍길동",
                    LocalDate.of(2026, 8, 15), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                    "소개", "url", 100000L);

            funding.updateBasicInfo("새 제목", LocalDate.of(2026, 8, 20),
                    LocalDate.of(2026, 7, 5), LocalDate.of(2026, 8, 1), "새 소개", "새url");

            assertThat(funding.getTitle()).isEqualTo("새 제목");
            assertThat(funding.getStartDate()).isEqualTo(LocalDate.of(2026, 7, 5));
        }

        @Test
        @DisplayName("시작일이 종료일보다 늦으면 예외가 발생하고 기존 값이 유지된다")
        void updateBasicInfo_fail_invalidPeriod_keepsOriginalValue() {
            Funding funding = Funding.createMyGift(1L, 1L, "원래제목", "홍길동",
                    LocalDate.of(2026, 8, 15), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                    "소개", "url", 100000L);

            assertThatThrownBy(() -> funding.updateBasicInfo(
                    "새 제목", LocalDate.of(2026, 8, 20),
                    LocalDate.of(2026, 8, 1), LocalDate.of(2026, 7, 1),
                    "새 소개", "새url"
            )).isInstanceOf(FundingException.class);

            assertThat(funding.getTitle()).isEqualTo("원래제목");
        }

        @Test
        @DisplayName("시작일과 종료일이 같아도 수정 가능하다")
        void updateBasicInfo_success_sameDates() {
            Funding funding = Funding.createMyGift(1L, 1L, "제목", "홍길동",
                    LocalDate.of(2026, 8, 15), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                    "소개", "url", 100000L);

            funding.updateBasicInfo("제목", LocalDate.of(2026, 8, 15),
                    LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 10), "소개", "url");

            assertThat(funding.getStartDate()).isEqualTo(funding.getEndDate());
        }
    }

    @Nested
    @DisplayName("기여 금액 수정")
    class UpdateContributionAmount {

        @Test
        @DisplayName("개설자가 정상 금액으로 수정하면 반영된다")
        void updateContributionAmount_success() {
            Long userId = 1L;
            Long fundingId = 10L;
            Long contributionId = 45L;

            Funding funding = Funding.createMyGift(userId, 5L, "제목", "홍길동",
                    LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(30),
                    "소개", "url", 100000L);
            ReflectionTestUtils.setField(funding, "id", fundingId);

            FundingContribution contribution = FundingContribution.createForGuest(
                    fundingId, 1L, "친구", false, 50000L, "축하해!", true
            );
            ReflectionTestUtils.setField(contribution, "id", contributionId);

            given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
            given(fundingContributionRepository.findById(contributionId)).willReturn(Optional.of(contribution));

            FundingContributionAmountUpdateResponse result = fundingService.updateContributionAmount(
                    userId, fundingId, contributionId, new FundingContributionAmountUpdateRequest(60000L)
            );

            assertThat(result.amount()).isEqualTo(60000L);
        }

        @Test
        @DisplayName("다른 펀딩 소속의 contributionId면 CONTRIBUTION_NOT_FOUND 예외가 발생한다")
        void updateContributionAmount_fail_wrongFunding() {
            Long userId = 1L;
            Long fundingId = 10L;
            Long contributionId = 45L;
            Long otherFundingId = 999L;

            Funding funding = Funding.createMyGift(userId, 5L, "제목", "홍길동",
                    LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(30),
                    "소개", "url", 100000L);
            ReflectionTestUtils.setField(funding, "id", fundingId);

            FundingContribution contribution = FundingContribution.createForGuest(
                    otherFundingId, 1L, "친구", false, 50000L, "축하해!", true  // 다른 펀딩 소속
            );
            ReflectionTestUtils.setField(contribution, "id", contributionId);

            given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
            given(fundingContributionRepository.findById(contributionId)).willReturn(Optional.of(contribution));

            assertThatThrownBy(() -> fundingService.updateContributionAmount(
                    userId, fundingId, contributionId, new FundingContributionAmountUpdateRequest(60000L)
            ))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.CONTRIBUTION_NOT_FOUND);
        }

        @Test
        @DisplayName("개설자가 아니면 NOT_FUNDING_OWNER 예외가 발생한다")
        void updateContributionAmount_fail_notOwner() {
            Long ownerId = 1L;
            Long requesterId = 2L;
            Long fundingId = 10L;

            Funding funding = Funding.createMyGift(ownerId, 5L, "제목", "홍길동",
                    LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(30),
                    "소개", "url", 100000L);
            ReflectionTestUtils.setField(funding, "id", fundingId);

            given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));

            assertThatThrownBy(() -> fundingService.updateContributionAmount(
                    requesterId, fundingId, 45L, new FundingContributionAmountUpdateRequest(60000L)
            ))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.NOT_FUNDING_OWNER);
        }
    }
}