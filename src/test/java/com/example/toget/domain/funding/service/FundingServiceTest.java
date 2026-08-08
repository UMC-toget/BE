package com.example.toget.domain.funding.service;

import com.example.toget.domain.funding.dto.request.FundingContributionAmountUpdateRequest;
import com.example.toget.domain.funding.dto.request.FundingSettlementContributionCreateRequest;
import com.example.toget.domain.funding.dto.request.FundingVisibilityUpdateRequest;
import com.example.toget.domain.funding.dto.response.FundingContributionAmountUpdateResponse;
import com.example.toget.domain.funding.dto.response.FundingMemberJoinResponse;
import com.example.toget.domain.funding.dto.response.FundingSettlementContributionCreateResponse;
import com.example.toget.domain.funding.dto.response.FundingVisibilityUpdateResponse;
import com.example.toget.domain.funding.entity.ContributionBackground;
import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.entity.FundingContribution;
import com.example.toget.domain.funding.entity.FundingMember;
import com.example.toget.domain.funding.entity.FundingVisibilitySettings;
import com.example.toget.domain.funding.enums.FundingRole;
import com.example.toget.domain.funding.enums.FundingStatus;
import com.example.toget.domain.funding.enums.SettlementStatus;
import com.example.toget.domain.funding.exception.ContributionException;
import com.example.toget.domain.funding.exception.FundingException;
import com.example.toget.domain.funding.exception.code.ContributionErrorCode;
import com.example.toget.domain.funding.exception.code.FundingErrorCode;
import com.example.toget.domain.funding.repository.ContributionBackgroundRepository;
import com.example.toget.domain.funding.repository.FundingContributionRepository;
import com.example.toget.domain.funding.repository.FundingGiftVoteRepository;
import com.example.toget.domain.funding.repository.FundingMemberRepository;
import com.example.toget.domain.funding.repository.FundingRepository;
import com.example.toget.domain.funding.repository.FundingVisibilitySettingsRepository;
import com.example.toget.domain.funding.dto.response.FundingAccountResponse;
import com.example.toget.domain.user.entity.UserAccount;
import com.example.toget.domain.user.repository.UserAccountRepository;
import com.example.toget.global.enums.BankName;
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

    @Mock
    private FundingMemberRepository fundingMemberRepository;

    @Mock
    private FundingGiftVoteRepository fundingGiftVoteRepository;

    @Mock
    private ContributionBackgroundRepository contributionBackgroundRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

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

    @Nested
    @DisplayName("함께 선물하기 참여(합류)")
    class Join {

        private static final Long OWNER_ID = 1L;
        private static final Long NEW_MEMBER_ID = 2L;
        private static final Long FUNDING_ID = 10L;

        private Funding togetherGiftFunding() {
            Funding funding = Funding.createTogetherGift(OWNER_ID, null, "제목", "홍길동",
                    LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(30),
                    "소개", "url", 100000L);
            ReflectionTestUtils.setField(funding, "id", FUNDING_ID);
            return funding;
        }

        @Test
        @DisplayName("아직 참여하지 않은 회원이면 PARTICIPANT로 합류한다")
        void join_success() {
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(togetherGiftFunding()));
            given(fundingMemberRepository.findByFundingIdAndUserId(FUNDING_ID, NEW_MEMBER_ID))
                    .willReturn(Optional.empty());
            given(fundingMemberRepository.save(any(FundingMember.class))).willAnswer(invocation -> {
                FundingMember member = invocation.getArgument(0);
                ReflectionTestUtils.setField(member, "id", 99L);
                return member;
            });

            FundingMemberJoinResponse result = fundingService.join(NEW_MEMBER_ID, FUNDING_ID);

            assertThat(result.fundingId()).isEqualTo(FUNDING_ID);
            assertThat(result.memberId()).isEqualTo(99L);

            ArgumentCaptor<FundingMember> captor = ArgumentCaptor.forClass(FundingMember.class);
            verify(fundingMemberRepository).save(captor.capture());
            assertThat(captor.getValue().getRole()).isEqualTo(FundingRole.PARTICIPANT);
            assertThat(captor.getValue().getUserId()).isEqualTo(NEW_MEMBER_ID);
        }

        @Test
        @DisplayName("펀딩이 없으면 FUNDING_NOT_FOUND 예외가 발생한다")
        void join_fail_fundingNotFound() {
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> fundingService.join(NEW_MEMBER_ID, FUNDING_ID))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.FUNDING_NOT_FOUND);
        }

        @Test
        @DisplayName("MY_GIFT 펀딩이면 NOT_TOGETHER_GIFT_TYPE 예외가 발생한다")
        void join_fail_notTogetherGiftType() {
            Funding funding = Funding.createMyGift(OWNER_ID, 5L, "제목", "홍길동",
                    LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(30),
                    "소개", "url", 100000L);
            ReflectionTestUtils.setField(funding, "id", FUNDING_ID);

            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(funding));

            assertThatThrownBy(() -> fundingService.join(NEW_MEMBER_ID, FUNDING_ID))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.NOT_TOGETHER_GIFT_TYPE);

            verify(fundingMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 참여 중인 회원이면 ALREADY_FUNDING_MEMBER 예외가 발생한다")
        void join_fail_alreadyMember() {
            FundingMember existing = FundingMember.createParticipant(FUNDING_ID, NEW_MEMBER_ID);

            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(togetherGiftFunding()));
            given(fundingMemberRepository.findByFundingIdAndUserId(FUNDING_ID, NEW_MEMBER_ID))
                    .willReturn(Optional.of(existing));

            assertThatThrownBy(() -> fundingService.join(NEW_MEMBER_ID, FUNDING_ID))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.ALREADY_FUNDING_MEMBER);

            verify(fundingMemberRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("함께 선물하기 나가기")
    class Leave {

        private static final Long OWNER_ID = 1L;
        private static final Long PARTICIPANT_ID = 2L;
        private static final Long FUNDING_ID = 10L;
        private static final Long MEMBER_ID = 55L;

        private Funding togetherGiftFunding() {
            Funding funding = Funding.createTogetherGift(OWNER_ID, null, "제목", "홍길동",
                    LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(30),
                    "소개", "url", 100000L);
            ReflectionTestUtils.setField(funding, "id", FUNDING_ID);
            return funding;
        }

        private FundingMember participantMember() {
            FundingMember member = FundingMember.createParticipant(FUNDING_ID, PARTICIPANT_ID);
            ReflectionTestUtils.setField(member, "id", MEMBER_ID);
            return member;
        }

        @Test
        @DisplayName("SELECTING 상태의 참여자는 나갈 수 있고, 남긴 투표도 함께 삭제된다")
        void leave_success() {
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(togetherGiftFunding()));
            given(fundingMemberRepository.findByFundingIdAndUserId(FUNDING_ID, PARTICIPANT_ID))
                    .willReturn(Optional.of(participantMember()));

            fundingService.leave(PARTICIPANT_ID, FUNDING_ID);

            verify(fundingGiftVoteRepository).deleteAllByFundingMemberId(MEMBER_ID);
            verify(fundingMemberRepository).delete(any(FundingMember.class));
        }

        @Test
        @DisplayName("펀딩이 없으면 FUNDING_NOT_FOUND 예외가 발생한다")
        void leave_fail_fundingNotFound() {
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> fundingService.leave(PARTICIPANT_ID, FUNDING_ID))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.FUNDING_NOT_FOUND);
        }

        @Test
        @DisplayName("본인의 멤버십이 없으면 MEMBER_NOT_FOUND 예외가 발생한다")
        void leave_fail_memberNotFound() {
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(togetherGiftFunding()));
            given(fundingMemberRepository.findByFundingIdAndUserId(FUNDING_ID, PARTICIPANT_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> fundingService.leave(PARTICIPANT_ID, FUNDING_ID))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("개설자는 나갈 수 없고 CREATOR_CANNOT_LEAVE_FUNDING 예외가 발생한다")
        void leave_fail_creatorCannotLeave() {
            FundingMember creator = FundingMember.createCreator(FUNDING_ID, OWNER_ID);
            ReflectionTestUtils.setField(creator, "id", MEMBER_ID);

            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(togetherGiftFunding()));
            given(fundingMemberRepository.findByFundingIdAndUserId(FUNDING_ID, OWNER_ID))
                    .willReturn(Optional.of(creator));

            assertThatThrownBy(() -> fundingService.leave(OWNER_ID, FUNDING_ID))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.CREATOR_CANNOT_LEAVE_FUNDING);

            verify(fundingMemberRepository, never()).delete(any());
        }

        @Test
        @DisplayName("SELECTING 상태가 아니면 INVALID_FUNDING_STATUS_FOR_LEAVE 예외가 발생한다")
        void leave_fail_invalidStatus() {
            Funding funding = togetherGiftFunding();
            ReflectionTestUtils.setField(funding, "status", FundingStatus.SETTLING);

            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(funding));
            given(fundingMemberRepository.findByFundingIdAndUserId(FUNDING_ID, PARTICIPANT_ID))
                    .willReturn(Optional.of(participantMember()));

            assertThatThrownBy(() -> fundingService.leave(PARTICIPANT_ID, FUNDING_ID))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.INVALID_FUNDING_STATUS_FOR_LEAVE);

            verify(fundingMemberRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("정산 참여자 입금 완료 신고")
    class ReportSettlementPayment {

        private static final Long OWNER_ID = 1L;
        private static final Long PARTICIPANT_ID = 2L;
        private static final Long FUNDING_ID = 10L;
        private static final Long MEMBER_ID = 55L;
        private static final Long BACKGROUND_ID = 3L;
        private static final Long AMOUNT_DUE = 25000L;

        private Funding togetherGiftFunding() {
            Funding funding = Funding.createTogetherGift(OWNER_ID, null, "제목", "홍길동",
                    LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(30),
                    "소개", "url", 100000L);
            ReflectionTestUtils.setField(funding, "id", FUNDING_ID);
            return funding;
        }

        /** 정산 확정으로 amountDue가 세팅되어 UNPAID 상태인 멤버 */
        private FundingMember settlementTargetMember() {
            FundingMember member = FundingMember.createParticipant(FUNDING_ID, PARTICIPANT_ID);
            ReflectionTestUtils.setField(member, "id", MEMBER_ID);
            member.confirmSettlement(AMOUNT_DUE);
            return member;
        }

        private ContributionBackground background() {
            ContributionBackground background = ContributionBackground.create("핑크", "#FFB6C1");
            ReflectionTestUtils.setField(background, "id", BACKGROUND_ID);
            return background;
        }

        private FundingSettlementContributionCreateRequest request() {
            return new FundingSettlementContributionCreateRequest(BACKGROUND_ID, "축하해!", false);
        }

        @Test
        @DisplayName("정산 대상자가 입금을 신고하면 UNPAID→PAID로 전환되고 참여 기록이 남는다")
        void reportSettlementPayment_success() {
            FundingMember member = settlementTargetMember();

            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(togetherGiftFunding()));
            given(fundingMemberRepository.findByFundingIdAndUserIdForUpdate(FUNDING_ID, PARTICIPANT_ID))
                    .willReturn(Optional.of(member));
            given(contributionBackgroundRepository.findById(BACKGROUND_ID))
                    .willReturn(Optional.of(background()));

            FundingSettlementContributionCreateResponse result =
                    fundingService.reportSettlementPayment(PARTICIPANT_ID, FUNDING_ID, request());

            assertThat(result.fundingId()).isEqualTo(FUNDING_ID);
            assertThat(result.memberId()).isEqualTo(MEMBER_ID);
            assertThat(result.settlementStatus()).isEqualTo(SettlementStatus.PAID.name());
            assertThat(member.getSettlementStatus()).isEqualTo(SettlementStatus.PAID);

            ArgumentCaptor<FundingContribution> captor = ArgumentCaptor.forClass(FundingContribution.class);
            verify(fundingContributionRepository).save(captor.capture());
            FundingContribution saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(PARTICIPANT_ID);
            assertThat(saved.getAmount()).isEqualTo(AMOUNT_DUE);
            assertThat(saved.getIsAnonymous()).isFalse();
        }

        @Test
        @DisplayName("펀딩이 없으면 FUNDING_NOT_FOUND 예외가 발생한다")
        void reportSettlementPayment_fail_fundingNotFound() {
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    fundingService.reportSettlementPayment(PARTICIPANT_ID, FUNDING_ID, request()))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.FUNDING_NOT_FOUND);
        }

        @Test
        @DisplayName("MY_GIFT 펀딩이면 NOT_TOGETHER_GIFT_TYPE 예외가 발생한다")
        void reportSettlementPayment_fail_notTogetherGiftType() {
            Funding funding = Funding.createMyGift(OWNER_ID, 5L, "제목", "홍길동",
                    LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(30),
                    "소개", "url", 100000L);
            ReflectionTestUtils.setField(funding, "id", FUNDING_ID);

            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(funding));

            assertThatThrownBy(() ->
                    fundingService.reportSettlementPayment(PARTICIPANT_ID, FUNDING_ID, request()))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.NOT_TOGETHER_GIFT_TYPE);

            verify(fundingContributionRepository, never()).save(any());
        }

        @Test
        @DisplayName("본인의 멤버십이 없으면 MEMBER_NOT_FOUND 예외가 발생한다")
        void reportSettlementPayment_fail_memberNotFound() {
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(togetherGiftFunding()));
            given(fundingMemberRepository.findByFundingIdAndUserIdForUpdate(FUNDING_ID, PARTICIPANT_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    fundingService.reportSettlementPayment(PARTICIPANT_ID, FUNDING_ID, request()))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("존재하지 않는 배경색을 지정하면 BACKGROUND_NOT_FOUND 예외가 발생한다")
        void reportSettlementPayment_fail_backgroundNotFound() {
            FundingMember member = settlementTargetMember();

            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(togetherGiftFunding()));
            given(fundingMemberRepository.findByFundingIdAndUserIdForUpdate(FUNDING_ID, PARTICIPANT_ID))
                    .willReturn(Optional.of(member));
            given(contributionBackgroundRepository.findById(BACKGROUND_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    fundingService.reportSettlementPayment(PARTICIPANT_ID, FUNDING_ID, request()))
                    .isInstanceOf(ContributionException.class)
                    .extracting(e -> ((ContributionException) e).getCode())
                    .isEqualTo(ContributionErrorCode.BACKGROUND_NOT_FOUND);

            // 배경 검증에서 막히므로 멤버 상태는 그대로여야 한다
            assertThat(member.getSettlementStatus()).isEqualTo(SettlementStatus.UNPAID);
            verify(fundingContributionRepository, never()).save(any());
        }

        @Test
        @DisplayName("정산 대상자로 확정되지 않았으면(amountDue 없음) NOT_SETTLEMENT_TARGET 예외가 발생한다")
        void reportSettlementPayment_fail_notSettlementTarget() {
            FundingMember member = FundingMember.createParticipant(FUNDING_ID, PARTICIPANT_ID);
            ReflectionTestUtils.setField(member, "id", MEMBER_ID);

            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(togetherGiftFunding()));
            given(fundingMemberRepository.findByFundingIdAndUserIdForUpdate(FUNDING_ID, PARTICIPANT_ID))
                    .willReturn(Optional.of(member));
            given(contributionBackgroundRepository.findById(BACKGROUND_ID))
                    .willReturn(Optional.of(background()));

            assertThatThrownBy(() ->
                    fundingService.reportSettlementPayment(PARTICIPANT_ID, FUNDING_ID, request()))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.NOT_SETTLEMENT_TARGET);

            verify(fundingContributionRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 입금 신고했거나 확인까지 끝난 경우 INVALID_SETTLEMENT_STATUS_TRANSITION 예외가 발생한다")
        void reportSettlementPayment_fail_alreadyReported() {
            FundingMember member = settlementTargetMember();
            member.requestPaymentConfirmation(); // UNPAID -> PAID로 미리 이동시켜 재신고 상황을 재현

            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(togetherGiftFunding()));
            given(fundingMemberRepository.findByFundingIdAndUserIdForUpdate(FUNDING_ID, PARTICIPANT_ID))
                    .willReturn(Optional.of(member));
            given(contributionBackgroundRepository.findById(BACKGROUND_ID))
                    .willReturn(Optional.of(background()));

            assertThatThrownBy(() ->
                    fundingService.reportSettlementPayment(PARTICIPANT_ID, FUNDING_ID, request()))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.INVALID_SETTLEMENT_STATUS_TRANSITION);

            verify(fundingContributionRepository, never()).save(any());
        }
    }

    /**
     * 정산 계좌 조회 — 개설자 검증 없이 누구나(비회원 포함) 조회 가능해야 한다.
     * shared-fundings/invitations와 같은 접근 정책이라, 회원인지 아닌지는 이 메서드 시그니처에
     * userId 자체가 없어졌다는 사실로 이미 보장된다 — 소유자 체크가 되살아나면 컴파일이 깨진다.
     */
    @Nested
    @DisplayName("정산 계좌 조회")
    class GetAccount {

        private static final Long FUNDING_ID = 20L;
        private static final Long USER_ACCOUNT_ID = 5L;

        private Funding myGiftFundingWithAccount() {
            Funding funding = Funding.createMyGift(1L, USER_ACCOUNT_ID, "제목", "홍길동",
                    LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(30),
                    "소개", "url", 100000L);
            ReflectionTestUtils.setField(funding, "id", FUNDING_ID);
            return funding;
        }

        private UserAccount account() {
            return UserAccount.builder()
                    .userId(1L)
                    .bankName(BankName.KB)
                    .accountOwner("홍길동")
                    .account("110-123-456789")
                    .build();
        }

        @Test
        @DisplayName("개설자가 아니어도(비회원 포함) 계좌 정보를 조회할 수 있다")
        void getAccount_success_evenForNonOwner() {
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID))
                    .willReturn(Optional.of(myGiftFundingWithAccount()));
            given(userAccountRepository.findWithBankById(USER_ACCOUNT_ID))
                    .willReturn(Optional.of(account()));

            FundingAccountResponse result = fundingService.getAccount(FUNDING_ID);

            assertThat(result.account()).isEqualTo("110-123-456789");
            assertThat(result.accountOwner()).isEqualTo("홍길동");
            assertThat(result.bankName()).isEqualTo("KB");
        }

        @Test
        @DisplayName("펀딩이 없거나 삭제됐으면 FUNDING_NOT_FOUND 예외가 발생한다")
        void getAccount_fail_fundingNotFound() {
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> fundingService.getAccount(FUNDING_ID))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.FUNDING_NOT_FOUND);
        }

        @Test
        @DisplayName("계좌가 아직 등록되지 않았으면 ACCOUNT_NOT_REGISTERED 예외가 발생한다")
        void getAccount_fail_accountNotRegistered() {
            Funding funding = Funding.createTogetherGift(1L, null, "제목", "홍길동",
                    LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(30),
                    "소개", "url", 100000L); // TOGETHER_GIFT는 계좌 없이도 생성 가능
            ReflectionTestUtils.setField(funding, "id", FUNDING_ID);
            given(fundingRepository.findByIdAndDeletedAtIsNull(FUNDING_ID)).willReturn(Optional.of(funding));

            assertThatThrownBy(() -> fundingService.getAccount(FUNDING_ID))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.ACCOUNT_NOT_REGISTERED);
        }
    }
}