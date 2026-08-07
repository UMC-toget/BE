package com.example.toget.domain.funding.service;

import com.example.toget.domain.funding.dto.FundingCollectedAmount;
import com.example.toget.domain.funding.dto.MyFundingListResponse;
import com.example.toget.domain.funding.dto.response.FundingTogetherGiftDashboardResponse;
import com.example.toget.domain.funding.dto.response.SharedFundingDetailResponse;
import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.entity.FundingMember;
import com.example.toget.domain.funding.entity.FundingVisibilitySettings;
import com.example.toget.domain.funding.exception.FundingException;
import com.example.toget.domain.funding.exception.code.FundingErrorCode;
import com.example.toget.domain.funding.repository.FundingContributionRepository;
import com.example.toget.domain.funding.repository.FundingGiftVoteRepository;
import com.example.toget.domain.funding.repository.FundingMemberRepository;
import com.example.toget.domain.funding.repository.FundingRepository;
import com.example.toget.domain.funding.repository.FundingVisibilitySettingsRepository;
import com.example.toget.domain.gift.enums.FundingGiftStatus;
import com.example.toget.domain.gift.repository.FundingGiftRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
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

    @Mock
    private FundingVisibilitySettingsRepository fundingVisibilitySettingsRepository;

    @Mock
    private FundingGiftRepository fundingGiftRepository;

    @Mock
    private FundingMemberRepository fundingMemberRepository;

    @Mock
    private FundingGiftVoteRepository fundingGiftVoteRepository;

    @Mock
    private FundingMemberUserResolver fundingMemberUserResolver;

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
        given(fundingRepository.findMyHostedFundings(eq(1L), any(Pageable.class)))
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
        given(fundingRepository.findMyHostedFundings(eq(1L), any(Pageable.class)))
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
        given(fundingRepository.findMyHostedFundings(eq(1L), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of(), Pageable.ofSize(10), false));

        MyFundingListResponse response = fundingQueryService.getMyFundings(1L, 0, 10);

        assertThat(response.fundings()).isEmpty();
        assertThat(response.hasNext()).isFalse();
        verify(fundingContributionRepository, never()).sumAmountsByFundingIds(anyList());
    }

    @Test
    @DisplayName("음수 page는 0으로, 0 이하 size는 10으로 보정해 조회한다")
    void normalizesInvalidPaging() {
        given(fundingRepository.findMyHostedFundings(eq(1L), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of(), Pageable.ofSize(10), false));

        fundingQueryService.getMyFundings(1L, -3, 0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(fundingRepository).findMyHostedFundings(eq(1L), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
    }

    /**
     * 외부 방문자용 상세 조회 — 공개 설정에 따른 null 마스킹이 핵심이다.
     *
     * [설계 포인트]
     *  - 토글 하나를 껐을 때 해당 필드만 가려지고 나머지는 살아 있어야 한다.
     *    그래서 각 테스트에서 "가려진 것"과 "안 가려진 것"을 함께 검증한다.
     *  - visibility 플래그는 마스킹 여부와 무관하게 원본 그대로 전달되어야 한다.
     *    프론트가 "비공개"와 "값 없음"을 구분하는 유일한 근거이기 때문.
     */
    @Nested
    @DisplayName("외부 방문자용 선물 준비 상세 조회")
    class GetSharedFundingDetail {

        private static final Long FUNDING_ID = 12L;

        private void givenFundingWithAggregates(Funding funding) {
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(funding));
            given(fundingContributionRepository.sumAmountByFundingId(FUNDING_ID)).willReturn(650_000L);
            given(fundingContributionRepository.countByFundingId(FUNDING_ID)).willReturn(12);
            given(fundingGiftRepository.findAllByFundingId(FUNDING_ID)).willReturn(List.of());
        }

        /** create(fundingId, 진행률, 참여자 수, 참여자 이름, 메시지, 모금액) — 인자 순서 주의 */
        private void givenVisibility(boolean progress, boolean amount, boolean participantCount) {
            given(fundingVisibilitySettingsRepository.findByFundingId(FUNDING_ID))
                    .willReturn(Optional.of(FundingVisibilitySettings.create(
                            FUNDING_ID, progress, participantCount, true, true, amount
                    )));
        }

        @Test
        @DisplayName("전체 공개면 모든 값이 그대로 내려온다")
        void allVisible() {
            givenFundingWithAggregates(fundingWithId(FUNDING_ID));
            givenVisibility(true, true, true);

            SharedFundingDetailResponse result = fundingQueryService.getSharedFundingDetail(FUNDING_ID);

            assertThat(result.collectedAmount()).isEqualTo(650_000L);
            assertThat(result.progressRate()).isEqualTo(65.0);
            assertThat(result.participantCount()).isEqualTo(12);
            assertThat(result.recipientName()).isEqualTo("홍길동");
            assertThat(result.status()).isEqualTo("SETTLING");
        }

        @Test
        @DisplayName("showProgress=false면 progressRate만 null이 된다")
        void hidesProgressRateOnly() {
            givenFundingWithAggregates(fundingWithId(FUNDING_ID));
            givenVisibility(false, true, true);

            SharedFundingDetailResponse result = fundingQueryService.getSharedFundingDetail(FUNDING_ID);

            assertThat(result.progressRate()).isNull();
            assertThat(result.collectedAmount()).isEqualTo(650_000L);
            assertThat(result.participantCount()).isEqualTo(12);
        }

        @Test
        @DisplayName("showAmount=false면 collectedAmount만 null이 된다")
        void hidesCollectedAmountOnly() {
            givenFundingWithAggregates(fundingWithId(FUNDING_ID));
            givenVisibility(true, false, true);

            SharedFundingDetailResponse result = fundingQueryService.getSharedFundingDetail(FUNDING_ID);

            assertThat(result.collectedAmount()).isNull();
            assertThat(result.progressRate()).isEqualTo(65.0);
            assertThat(result.participantCount()).isEqualTo(12);
            // 목표 금액은 공개 설정 대상이 아니다
            assertThat(result.targetAmount()).isEqualTo(1_000_000L);
        }

        @Test
        @DisplayName("showParticipantCount=false면 participantCount만 null이 된다")
        void hidesParticipantCountOnly() {
            givenFundingWithAggregates(fundingWithId(FUNDING_ID));
            givenVisibility(true, true, false);

            SharedFundingDetailResponse result = fundingQueryService.getSharedFundingDetail(FUNDING_ID);

            assertThat(result.participantCount()).isNull();
            assertThat(result.collectedAmount()).isEqualTo(650_000L);
            assertThat(result.progressRate()).isEqualTo(65.0);
        }

        @Test
        @DisplayName("값이 가려져도 visibility 플래그는 원본 그대로 전달된다")
        void visibilityFlagsArePreserved() {
            givenFundingWithAggregates(fundingWithId(FUNDING_ID));
            givenVisibility(false, false, false);

            SharedFundingDetailResponse result = fundingQueryService.getSharedFundingDetail(FUNDING_ID);

            assertThat(result.visibility().showProgress()).isFalse();
            assertThat(result.visibility().showAmount()).isFalse();
            assertThat(result.visibility().showParticipantCount()).isFalse();
            // 이 API가 마스킹하지 않는 두 토글도 값은 그대로 전달된다
            assertThat(result.visibility().showParticipantNames()).isTrue();
            assertThat(result.visibility().showMessages()).isTrue();
        }

        @Test
        @DisplayName("공개 설정 행이 없으면 전체 공개로 처리된다")
        void noVisibilitySettings_treatedAsFullyOpen() {
            givenFundingWithAggregates(fundingWithId(FUNDING_ID));
            given(fundingVisibilitySettingsRepository.findByFundingId(FUNDING_ID))
                    .willReturn(Optional.empty());

            SharedFundingDetailResponse result = fundingQueryService.getSharedFundingDetail(FUNDING_ID);

            assertThat(result.collectedAmount()).isEqualTo(650_000L);
            assertThat(result.progressRate()).isEqualTo(65.0);
            assertThat(result.participantCount()).isEqualTo(12);
            assertThat(result.visibility().showProgress()).isTrue();
        }

        @Test
        @DisplayName("펀딩이 없으면 FUNDING_NOT_FOUND 예외가 발생한다")
        void fundingNotFound() {
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> fundingQueryService.getSharedFundingDetail(FUNDING_ID))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.FUNDING_NOT_FOUND);
        }

        @Test
        @DisplayName("TOGETHER_GIFT 펀딩이면 NOT_MY_GIFT_TYPE 예외가 발생한다")
        void notMyGiftType() {
            Funding together = Funding.createTogetherGift(1L, null, "제목", "홍길동",
                    LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(30),
                    "소개", "url", 100_000L);
            ReflectionTestUtils.setField(together, "id", FUNDING_ID);
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(together));

            assertThatThrownBy(() -> fundingQueryService.getSharedFundingDetail(FUNDING_ID))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.NOT_MY_GIFT_TYPE);
        }
    }

    /**
     * TOGETHER_GIFT 상세 조회 — issue #101: 개설자/공동관리자/일반참여자/비회원 모두 조회는 열려 있고,
     * 응답의 myRole로만 구분된다.
     *
     * [설계 포인트]
     *  - 조회자의 role 판단(myRole)만 검증 대상으로 좁히고, 상태별 필드 채움 로직(SELECTING 분기)은
     *    이 테스트의 관심사가 아니므로 후보 선물 없음으로 고정해 최소한으로만 스텁한다.
     */
    @Nested
    @DisplayName("함께 선물하기 상세 조회 — myRole")
    class GetTogetherGiftDashboard {

        private static final Long FUNDING_ID = 30L;
        private static final Long CREATOR_ID = 1L;

        private Funding togetherGiftFundingWithId(Long id) {
            Funding funding = Funding.createTogetherGift(CREATOR_ID, null, "길동이의 생일 펀딩", "홍길동",
                    LocalDate.of(2026, 8, 14), null, null, "소개글", "https://image.com/thumb.png", 1_000_000L);
            ReflectionTestUtils.setField(funding, "id", id);
            return funding;
        }

        /** myRole 판단과 무관한 조회(멤버 요약, SELECTING 분기 후보 선물)는 빈 값으로 고정 스텁 */
        private void stubRoleIndependentQueries() {
            given(fundingMemberRepository.findTopMembersOrderByRole(eq(FUNDING_ID), any(Pageable.class)))
                    .willReturn(List.of());
            given(fundingMemberUserResolver.resolve(anyList())).willReturn(Map.of());
            given(fundingGiftRepository.findAllByFundingIdAndStatus(FUNDING_ID, FundingGiftStatus.CANDIDATE))
                    .willReturn(List.of());
        }

        @Test
        @DisplayName("개설자가 조회하면 myRole=CREATOR")
        void creatorSeesOwnRole() {
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID))
                    .willReturn(Optional.of(togetherGiftFundingWithId(FUNDING_ID)));
            stubRoleIndependentQueries();
            given(fundingMemberRepository.findByFundingIdAndUserId(FUNDING_ID, CREATOR_ID))
                    .willReturn(Optional.of(FundingMember.createCreator(FUNDING_ID, CREATOR_ID)));

            FundingTogetherGiftDashboardResponse result =
                    fundingQueryService.getTogetherGiftDashboard(CREATOR_ID, FUNDING_ID);

            assertThat(result.myRole()).isEqualTo("CREATOR");
        }

        @Test
        @DisplayName("일반 참여자가 조회하면 myRole=PARTICIPANT")
        void participantSeesOwnRole() {
            Long participantId = 7L;
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID))
                    .willReturn(Optional.of(togetherGiftFundingWithId(FUNDING_ID)));
            stubRoleIndependentQueries();
            given(fundingMemberRepository.findByFundingIdAndUserId(FUNDING_ID, participantId))
                    .willReturn(Optional.of(FundingMember.createParticipant(FUNDING_ID, participantId)));

            FundingTogetherGiftDashboardResponse result =
                    fundingQueryService.getTogetherGiftDashboard(participantId, FUNDING_ID);

            assertThat(result.myRole()).isEqualTo("PARTICIPANT");
        }

        @Test
        @DisplayName("비회원(userId=null)이 조회하면 myRole=null이고 멤버 조회 자체를 하지 않는다")
        void guestSeesNullRole() {
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID))
                    .willReturn(Optional.of(togetherGiftFundingWithId(FUNDING_ID)));
            stubRoleIndependentQueries();

            FundingTogetherGiftDashboardResponse result =
                    fundingQueryService.getTogetherGiftDashboard(null, FUNDING_ID);

            assertThat(result.myRole()).isNull();
            verify(fundingMemberRepository, never()).findByFundingIdAndUserId(any(), any());
        }

        @Test
        @DisplayName("로그인했지만 아직 합류하지 않은 회원이 조회하면 myRole=null")
        void loggedInNonMemberSeesNullRole() {
            Long strangerId = 99L;
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID))
                    .willReturn(Optional.of(togetherGiftFundingWithId(FUNDING_ID)));
            stubRoleIndependentQueries();
            given(fundingMemberRepository.findByFundingIdAndUserId(FUNDING_ID, strangerId))
                    .willReturn(Optional.empty());

            FundingTogetherGiftDashboardResponse result =
                    fundingQueryService.getTogetherGiftDashboard(strangerId, FUNDING_ID);

            assertThat(result.myRole()).isNull();
        }

        @Test
        @DisplayName("펀딩이 없거나 삭제됐으면 FUNDING_NOT_FOUND 예외가 발생한다")
        void fundingNotFound() {
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> fundingQueryService.getTogetherGiftDashboard(CREATOR_ID, FUNDING_ID))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.FUNDING_NOT_FOUND);
        }

        @Test
        @DisplayName("MY_GIFT 펀딩이면 NOT_TOGETHER_GIFT_TYPE 예외가 발생한다")
        void notTogetherGiftType() {
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID))
                    .willReturn(Optional.of(fundingWithId(FUNDING_ID)));

            assertThatThrownBy(() -> fundingQueryService.getTogetherGiftDashboard(CREATOR_ID, FUNDING_ID))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.NOT_TOGETHER_GIFT_TYPE);
        }
    }

}
