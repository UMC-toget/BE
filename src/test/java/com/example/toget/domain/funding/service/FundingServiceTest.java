package com.example.toget.domain.funding.service;

import com.example.toget.domain.funding.dto.request.FundingContributionAmountUpdateRequest;
import com.example.toget.domain.funding.dto.request.FundingVisibilityUpdateRequest;
import com.example.toget.domain.funding.dto.response.FundingContributionAmountUpdateResponse;
import com.example.toget.domain.funding.dto.response.FundingVisibilityUpdateResponse;
import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.entity.FundingContribution;
import com.example.toget.domain.funding.entity.FundingVisibilitySettings;
import com.example.toget.domain.funding.enums.FundingStatus;
import com.example.toget.domain.funding.exception.FundingException;
import com.example.toget.domain.funding.exception.code.FundingErrorCode;
import com.example.toget.domain.funding.repository.FundingContributionRepository;
import com.example.toget.domain.funding.repository.FundingRepository;
import com.example.toget.domain.funding.repository.FundingVisibilitySettingsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * FundingService(쓰기 로직) 단위 테스트.
 * 기본정보/기여금액/공개 설정 수정 등 write 트랜잭션을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class FundingServiceTest {

    @Mock
    private FundingRepository fundingRepository;

    @Mock
    private FundingContributionRepository fundingContributionRepository;

    @Mock
    private FundingVisibilitySettingsRepository fundingVisibilitySettingsRepository;

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

    @Nested
    @DisplayName("공개 설정 수정")
    class UpdateVisibility {

        private static final Long OWNER_ID = 1L;
        private static final Long FUNDING_ID = 10L;

        private Funding myGiftFunding(Long ownerId) {
            Funding funding = Funding.createMyGift(ownerId, 5L, "제목", "홍길동",
                    LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(30),
                    "소개", "url", 100000L);
            ReflectionTestUtils.setField(funding, "id", FUNDING_ID);
            return funding;
        }

        /** 5개 값을 전부 다르게 조합해, 순서가 틀어지면 반드시 깨지도록 만든 요청 */
        private FundingVisibilityUpdateRequest distinctRequest() {
            return new FundingVisibilityUpdateRequest(
                    true,   // showProgress
                    false,  // showAmount
                    true,   // showParticipantCount
                    false,  // showParticipantNames
                    true    // showMessages
            );
        }

        /**
         * 인자 순서 회귀 방지 테스트.
         * 엔티티 update()는 (진행률, 참여자 수, 참여자 이름, 메시지, 모금액) 순이고 DTO는 모금액이 2번째라,
         * 호출부를 DTO 순서로 "정리"하면 값이 엉뚱한 컬럼에 저장된다.
         * 5개가 모두 Boolean이라 컴파일러가 못 잡으므로 이 테스트가 매핑을 고정한다.
         */
        @Test
        @DisplayName("각 토글 값이 대응하는 컬럼에 저장된다")
        void updateVisibility_success_fieldMapping() {
            Funding funding = myGiftFunding(OWNER_ID);
            FundingVisibilitySettings settings = FundingVisibilitySettings.create(
                    FUNDING_ID, false, false, false, false, false  // 전부 false에서 시작
            );
            ReflectionTestUtils.setField(settings, "id", 7L);

            given(fundingRepository.findById(FUNDING_ID)).willReturn(Optional.of(funding));
            given(fundingVisibilitySettingsRepository.findByFundingId(FUNDING_ID))
                    .willReturn(Optional.of(settings));

            FundingVisibilityUpdateResponse result =
                    fundingService.updateVisibility(OWNER_ID, FUNDING_ID, distinctRequest());

            // 엔티티(컬럼) 기준 검증 — 순서가 밀리면 여기서 깨진다
            assertThat(settings.getIsProgressVisible()).isTrue();
            assertThat(settings.getIsCollectedAmountVisible()).isFalse();
            assertThat(settings.getIsParticipantCountVisible()).isTrue();
            assertThat(settings.getIsParticipantNameVisible()).isFalse();
            assertThat(settings.getIsMessageVisible()).isTrue();

            // 응답 DTO 기준 검증 — 컨버터가 getter를 잘못 짝지어도 잡힌다
            assertThat(result.fundingVisibilitySettingId()).isEqualTo(7L);
            assertThat(result.showProgress()).isTrue();
            assertThat(result.showAmount()).isFalse();
            assertThat(result.showParticipantCount()).isTrue();
            assertThat(result.showParticipantNames()).isFalse();
            assertThat(result.showMessages()).isTrue();
        }

        @Test
        @DisplayName("설정 행이 없으면 생성해서 저장한다(upsert)")
        void updateVisibility_success_upsertWhenMissing() {
            Funding funding = myGiftFunding(OWNER_ID);

            given(fundingRepository.findById(FUNDING_ID)).willReturn(Optional.of(funding));
            given(fundingVisibilitySettingsRepository.findByFundingId(FUNDING_ID))
                    .willReturn(Optional.empty());
            given(fundingVisibilitySettingsRepository.save(any(FundingVisibilitySettings.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            FundingVisibilityUpdateResponse result =
                    fundingService.updateVisibility(OWNER_ID, FUNDING_ID, distinctRequest());

            ArgumentCaptor<FundingVisibilitySettings> captor =
                    ArgumentCaptor.forClass(FundingVisibilitySettings.class);
            verify(fundingVisibilitySettingsRepository).save(captor.capture());

            FundingVisibilitySettings saved = captor.getValue();
            assertThat(saved.getFundingId()).isEqualTo(FUNDING_ID);
            // 새로 만든 행에도 동일한 매핑이 적용되어야 한다
            assertThat(saved.getIsProgressVisible()).isTrue();
            assertThat(saved.getIsCollectedAmountVisible()).isFalse();
            assertThat(saved.getIsParticipantCountVisible()).isTrue();
            assertThat(saved.getIsParticipantNameVisible()).isFalse();
            assertThat(saved.getIsMessageVisible()).isTrue();

            assertThat(result.showProgress()).isTrue();
            assertThat(result.showAmount()).isFalse();
        }

        @Test
        @DisplayName("종료된 펀딩도 수정할 수 있다")
        void updateVisibility_success_whenEnded() {
            Funding funding = myGiftFunding(OWNER_ID);
            ReflectionTestUtils.setField(funding, "status", FundingStatus.ENDED);

            FundingVisibilitySettings settings = FundingVisibilitySettings.create(
                    FUNDING_ID, true, true, true, true, true
            );
            ReflectionTestUtils.setField(settings, "id", 7L);

            given(fundingRepository.findById(FUNDING_ID)).willReturn(Optional.of(funding));
            given(fundingVisibilitySettingsRepository.findByFundingId(FUNDING_ID))
                    .willReturn(Optional.of(settings));

            FundingVisibilityUpdateResponse result =
                    fundingService.updateVisibility(OWNER_ID, FUNDING_ID, distinctRequest());

            assertThat(result.showParticipantNames()).isFalse();
            assertThat(settings.getIsParticipantNameVisible()).isFalse();
        }

        @Test
        @DisplayName("펀딩이 없으면 FUNDING_NOT_FOUND 예외가 발생한다")
        void updateVisibility_fail_fundingNotFound() {
            given(fundingRepository.findById(FUNDING_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    fundingService.updateVisibility(OWNER_ID, FUNDING_ID, distinctRequest()))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.FUNDING_NOT_FOUND);
        }

        @Test
        @DisplayName("개설자가 아니면 NOT_FUNDING_OWNER 예외가 발생한다")
        void updateVisibility_fail_notOwner() {
            Long requesterId = 2L;
            given(fundingRepository.findById(FUNDING_ID)).willReturn(Optional.of(myGiftFunding(OWNER_ID)));

            assertThatThrownBy(() ->
                    fundingService.updateVisibility(requesterId, FUNDING_ID, distinctRequest()))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.NOT_FUNDING_OWNER);

            verify(fundingVisibilitySettingsRepository, never()).findByFundingId(any());
        }

        @Test
        @DisplayName("TOGETHER_GIFT 펀딩이면 NOT_MY_GIFT_TYPE 예외가 발생한다")
        void updateVisibility_fail_notMyGiftType() {
            Funding funding = Funding.createTogetherGift(OWNER_ID, null, "제목", "홍길동",
                    LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(30),
                    "소개", "url", 100000L);
            ReflectionTestUtils.setField(funding, "id", FUNDING_ID);

            given(fundingRepository.findById(FUNDING_ID)).willReturn(Optional.of(funding));

            assertThatThrownBy(() ->
                    fundingService.updateVisibility(OWNER_ID, FUNDING_ID, distinctRequest()))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.NOT_MY_GIFT_TYPE);

            // 유형 검증에서 막히므로 설정 행을 건드리지 않아야 한다
            verify(fundingVisibilitySettingsRepository, never()).findByFundingId(any());
        }
    }
}