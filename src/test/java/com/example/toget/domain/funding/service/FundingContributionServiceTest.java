package com.example.toget.domain.funding.service;

import com.example.toget.domain.funding.dto.response.FundingContributionDetailResponse;
import com.example.toget.domain.funding.dto.response.FundingContributionRollingPaperResponse;
import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.entity.FundingContribution;
import com.example.toget.domain.funding.entity.FundingVisibilitySettings;
import com.example.toget.domain.funding.repository.FundingContributionRepository;
import com.example.toget.domain.funding.repository.FundingRepository;
import com.example.toget.domain.funding.repository.FundingVisibilitySettingsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * FundingContributionService 조회 로직 단위 테스트.
 *
 * [설계 포인트]
 *  - 비회원(viewerId = null)이 개설자로 오인되지 않는지, 그리고 개설자가 설정한 공개 범위가
 *    축하 메시지 목록·상세에 모두 적용되는지를 고정한다.
 *  - 특히 상세 조회는 목록에서 가려진 메시지를 우회 조회하는 통로가 될 수 있어 별도로 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class FundingContributionServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long FUNDING_ID = 10L;
    private static final Long CONTRIBUTION_ID = 45L;

    @Mock
    private FundingRepository fundingRepository;

    @Mock
    private FundingContributionRepository fundingContributionRepository;

    @Mock
    private FundingVisibilitySettingsRepository fundingVisibilitySettingsRepository;

    @InjectMocks
    private FundingContributionService fundingContributionService;

    private Funding funding() {
        Funding funding = Funding.createMyGift(OWNER_ID, 5L, "제목", "홍길동",
                LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(30),
                "소개", "url", 100000L);
        ReflectionTestUtils.setField(funding, "id", FUNDING_ID);
        return funding;
    }

    /** 익명도 비밀편지도 아닌, 개인 설정상 전부 공개인 참여 기록 */
    private FundingContribution openContribution() {
        FundingContribution contribution = FundingContribution.createForGuest(
                FUNDING_ID, 1L, "친구", false, 50000L, "축하해!", true
        );
        ReflectionTestUtils.setField(contribution, "id", CONTRIBUTION_ID);
        return contribution;
    }

    private FundingVisibilitySettings visibility(boolean showParticipantNames, boolean showMessages) {
        // create(fundingId, 진행률, 참여자 수, 참여자 이름, 메시지, 모금액) — 인자 순서 주의
        return FundingVisibilitySettings.create(
                FUNDING_ID, true, true, showParticipantNames, showMessages, true
        );
    }

    @Nested
    @DisplayName("축하 메시지 목록 조회")
    class GetContributions {

        @Test
        @DisplayName("비회원이 조회하면 개설자로 취급되지 않아 익명 참여자 이름이 가려진다")
        void guest_isNotTreatedAsOwner() {
            FundingContribution anonymous = FundingContribution.createForGuest(
                    FUNDING_ID, 1L, "친구", true, 50000L, "축하해!", true  // 익명 참여
            );

            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(funding()));
            given(fundingContributionRepository.findAllByFundingId(FUNDING_ID))
                    .willReturn(List.of(anonymous));
            given(fundingVisibilitySettingsRepository.findByFundingId(FUNDING_ID))
                    .willReturn(Optional.of(visibility(true, true)));

            FundingContributionRollingPaperResponse result =
                    fundingContributionService.getContributions(FUNDING_ID, null);  // 비회원

            assertThat(result.contributions().get(0).senderName()).isNull();
        }

        @Test
        @DisplayName("showParticipantNames=false면 익명이 아닌 참여자 이름도 가려진다")
        void hidesSenderName_whenOwnerTurnedOff() {
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(funding()));
            given(fundingContributionRepository.findAllByFundingId(FUNDING_ID))
                    .willReturn(List.of(openContribution()));
            given(fundingVisibilitySettingsRepository.findByFundingId(FUNDING_ID))
                    .willReturn(Optional.of(visibility(false, true)));

            FundingContributionRollingPaperResponse result =
                    fundingContributionService.getContributions(FUNDING_ID, null);

            assertThat(result.contributions().get(0).senderName()).isNull();
            // 메시지는 켜져 있으므로 그대로 노출
            assertThat(result.contributions().get(0).content()).isEqualTo("축하해!");
        }

        @Test
        @DisplayName("showMessages=false면 비밀편지가 아닌 메시지도 가려진다")
        void hidesContent_whenOwnerTurnedOff() {
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(funding()));
            given(fundingContributionRepository.findAllByFundingId(FUNDING_ID))
                    .willReturn(List.of(openContribution()));
            given(fundingVisibilitySettingsRepository.findByFundingId(FUNDING_ID))
                    .willReturn(Optional.of(visibility(true, false)));

            FundingContributionRollingPaperResponse result =
                    fundingContributionService.getContributions(FUNDING_ID, null);

            assertThat(result.contributions().get(0).content()).isNull();
            // 이름은 켜져 있으므로 그대로 노출
            assertThat(result.contributions().get(0).senderName()).isEqualTo("친구");
        }

        @Test
        @DisplayName("개설자 본인이 조회하면 공개 설정이 모두 꺼져 있어도 원본이 그대로 보인다")
        void owner_seesOriginal() {
            FundingContribution anonymousPrivate = FundingContribution.createForGuest(
                    FUNDING_ID, 1L, "친구", true, 50000L, "축하해!", false  // 익명 + 비밀편지
            );

            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(funding()));
            given(fundingContributionRepository.findAllByFundingId(FUNDING_ID))
                    .willReturn(List.of(anonymousPrivate));
            given(fundingVisibilitySettingsRepository.findByFundingId(FUNDING_ID))
                    .willReturn(Optional.of(visibility(false, false)));

            FundingContributionRollingPaperResponse result =
                    fundingContributionService.getContributions(FUNDING_ID, OWNER_ID);

            assertThat(result.contributions().get(0).senderName()).isEqualTo("친구");
            assertThat(result.contributions().get(0).content()).isEqualTo("축하해!");
        }

        @Test
        @DisplayName("공개 설정이 없는 펀딩은 전체 공개로 처리된다")
        void noVisibilitySettings_treatedAsFullyOpen() {
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(funding()));
            given(fundingContributionRepository.findAllByFundingId(FUNDING_ID))
                    .willReturn(List.of(openContribution()));
            given(fundingVisibilitySettingsRepository.findByFundingId(FUNDING_ID))
                    .willReturn(Optional.empty());  // TOGETHER_GIFT 등

            FundingContributionRollingPaperResponse result =
                    fundingContributionService.getContributions(FUNDING_ID, null);

            assertThat(result.contributions().get(0).senderName()).isEqualTo("친구");
            assertThat(result.contributions().get(0).content()).isEqualTo("축하해!");
        }

        @Test
        @DisplayName("참여자 개인 설정과 개설자 설정은 각각 독립적으로 적용된다")
        void participantAndOwnerSettings_applyIndependently() {
            FundingContribution anonymous = FundingContribution.createForGuest(
                    FUNDING_ID, 1L, "친구", true, 50000L, "축하해!", true  // 익명이지만 편지는 공개
            );

            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(funding()));
            given(fundingContributionRepository.findAllByFundingId(FUNDING_ID))
                    .willReturn(List.of(anonymous));
            // 개설자는 이름 공개를 켜뒀지만, 참여자가 익명을 택했으므로 이름은 여전히 가려져야 한다
            given(fundingVisibilitySettingsRepository.findByFundingId(FUNDING_ID))
                    .willReturn(Optional.of(visibility(true, true)));

            FundingContributionRollingPaperResponse result =
                    fundingContributionService.getContributions(FUNDING_ID, null);

            assertThat(result.contributions().get(0).senderName()).isNull();
            assertThat(result.contributions().get(0).content()).isEqualTo("축하해!");
        }
    }

    @Nested
    @DisplayName("축하 메시지 상세 조회")
    class GetContributionDetail {

        /**
         * 목록에서 가려진 메시지를 상세 API로 우회 조회할 수 없어야 한다.
         * contributionId는 순차 증가라 추측이 어렵지 않으므로, 목록만 막으면 은닉이 무력화된다.
         */
        @Test
        @DisplayName("showMessages=false면 상세 조회로도 내용을 볼 수 없다")
        void cannotBypassViaDetail() {
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(funding()));
            given(fundingContributionRepository.findById(CONTRIBUTION_ID))
                    .willReturn(Optional.of(openContribution()));
            given(fundingVisibilitySettingsRepository.findByFundingId(FUNDING_ID))
                    .willReturn(Optional.of(visibility(true, false)));

            FundingContributionDetailResponse result =
                    fundingContributionService.getContributionDetail(FUNDING_ID, CONTRIBUTION_ID, null);

            assertThat(result.content()).isNull();
        }

        @Test
        @DisplayName("개설자 본인은 상세 조회에서 원본을 그대로 본다")
        void owner_seesOriginal() {
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(funding()));
            given(fundingContributionRepository.findById(CONTRIBUTION_ID))
                    .willReturn(Optional.of(openContribution()));
            given(fundingVisibilitySettingsRepository.findByFundingId(FUNDING_ID))
                    .willReturn(Optional.of(visibility(false, false)));

            FundingContributionDetailResponse result =
                    fundingContributionService.getContributionDetail(FUNDING_ID, CONTRIBUTION_ID, OWNER_ID);

            assertThat(result.content()).isEqualTo("축하해!");
        }

        @Test
        @DisplayName("공개 설정이 켜져 있으면 비회원도 내용을 볼 수 있다")
        void guest_seesContent_whenVisible() {
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(funding()));
            given(fundingContributionRepository.findById(CONTRIBUTION_ID))
                    .willReturn(Optional.of(openContribution()));
            given(fundingVisibilitySettingsRepository.findByFundingId(FUNDING_ID))
                    .willReturn(Optional.of(visibility(true, true)));

            FundingContributionDetailResponse result =
                    fundingContributionService.getContributionDetail(FUNDING_ID, CONTRIBUTION_ID, null);

            assertThat(result.content()).isEqualTo("축하해!");
        }
    }
}
