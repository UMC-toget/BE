package com.example.toget.domain.gift.service;

import com.example.toget.domain.funding.entity.FundingMember;
import com.example.toget.domain.funding.repository.FundingGiftVoteRepository;
import com.example.toget.domain.funding.repository.FundingMemberRepository;
import com.example.toget.domain.funding.repository.FundingRepository;
import com.example.toget.domain.gift.dto.response.FundingGiftVoteToggleResponse;
import com.example.toget.domain.gift.entity.FundingGift;
import com.example.toget.domain.gift.entity.FundingGiftVote;
import com.example.toget.domain.gift.exception.FundingGiftException;
import com.example.toget.domain.gift.exception.code.FundingGiftErrorCode;
import com.example.toget.domain.gift.repository.FundingGiftCommentRepository;
import com.example.toget.domain.gift.repository.FundingGiftPurchaseRepository;
import com.example.toget.domain.gift.repository.FundingGiftRepository;
import com.example.toget.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * FundingGiftService 단위 테스트.
 * toggleVote()의 동시 요청 방지(락 조회 사용) 및 기존 분기 동작을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class FundingGiftServiceTest {

    @Mock
    private FundingRepository fundingRepository;

    @Mock
    private FundingMemberRepository fundingMemberRepository;

    @Mock
    private FundingGiftRepository fundingGiftRepository;

    @Mock
    private FundingGiftVoteRepository fundingGiftVoteRepository;

    @Mock
    private FundingGiftCommentRepository fundingGiftCommentRepository;

    @Mock
    private FundingGiftPurchaseRepository fundingGiftPurchaseRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FundingGiftService fundingGiftService;

    @Nested
    @DisplayName("선물 후보 투표 토글")
    class ToggleVote {

        private static final Long USER_ID = 1L;
        private static final Long FUNDING_ID = 10L;
        private static final Long GIFT_ID = 100L;
        private static final Long MEMBER_ID = 55L;

        private FundingGift candidateGift() {
            FundingGift gift = FundingGift.createCandidate(
                    FUNDING_ID, 999L, "선물", 10000L, "url", "img", null);
            ReflectionTestUtils.setField(gift, "id", GIFT_ID);
            return gift;
        }

        private FundingMember member() {
            FundingMember member = FundingMember.createParticipant(FUNDING_ID, USER_ID);
            ReflectionTestUtils.setField(member, "id", MEMBER_ID);
            return member;
        }

        @Test
        @DisplayName("투표하지 않은 후보면 락을 건 멤버 조회로 새로 투표한다")
        void toggleVote_success_newVote() {
            given(fundingGiftRepository.findById(GIFT_ID)).willReturn(Optional.of(candidateGift()));
            given(fundingMemberRepository.findByFundingIdAndUserIdForUpdate(FUNDING_ID, USER_ID))
                    .willReturn(Optional.of(member()));
            given(fundingGiftVoteRepository.findByFundingMemberIdAndFundingGiftId(MEMBER_ID, GIFT_ID))
                    .willReturn(Optional.empty());
            given(fundingGiftVoteRepository.findAllByFundingMemberId(MEMBER_ID))
                    .willReturn(List.of());

            FundingGiftVoteToggleResponse result = fundingGiftService.toggleVote(USER_ID, FUNDING_ID, GIFT_ID);

            assertThat(result.fundingGiftId()).isEqualTo(GIFT_ID);
            assertThat(result.voted()).isTrue();
            verify(fundingGiftVoteRepository).save(any(FundingGiftVote.class));
            // 락 없는 기존 조회는 더 이상 이 경로에서 쓰이지 않아야 한다
            verify(fundingMemberRepository, never()).findByFundingIdAndUserId(any(), any());
        }

        @Test
        @DisplayName("이미 투표한 후보면 취소한다")
        void toggleVote_success_cancelVote() {
            FundingGiftVote existing = FundingGiftVote.create(GIFT_ID, MEMBER_ID);

            given(fundingGiftRepository.findById(GIFT_ID)).willReturn(Optional.of(candidateGift()));
            given(fundingMemberRepository.findByFundingIdAndUserIdForUpdate(FUNDING_ID, USER_ID))
                    .willReturn(Optional.of(member()));
            given(fundingGiftVoteRepository.findByFundingMemberIdAndFundingGiftId(MEMBER_ID, GIFT_ID))
                    .willReturn(Optional.of(existing));

            FundingGiftVoteToggleResponse result = fundingGiftService.toggleVote(USER_ID, FUNDING_ID, GIFT_ID);

            assertThat(result.voted()).isFalse();
            verify(fundingGiftVoteRepository).delete(existing);
            verify(fundingGiftVoteRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 3표를 채운 멤버가 새 후보에 투표하면 VOTE_LIMIT_EXCEEDED 예외가 발생한다")
        void toggleVote_fail_limitExceeded() {
            given(fundingGiftRepository.findById(GIFT_ID)).willReturn(Optional.of(candidateGift()));
            given(fundingMemberRepository.findByFundingIdAndUserIdForUpdate(FUNDING_ID, USER_ID))
                    .willReturn(Optional.of(member()));
            given(fundingGiftVoteRepository.findByFundingMemberIdAndFundingGiftId(MEMBER_ID, GIFT_ID))
                    .willReturn(Optional.empty());
            given(fundingGiftVoteRepository.findAllByFundingMemberId(MEMBER_ID))
                    .willReturn(List.of(
                            FundingGiftVote.create(101L, MEMBER_ID),
                            FundingGiftVote.create(102L, MEMBER_ID),
                            FundingGiftVote.create(103L, MEMBER_ID)
                    ));

            assertThatThrownBy(() -> fundingGiftService.toggleVote(USER_ID, FUNDING_ID, GIFT_ID))
                    .isInstanceOf(FundingGiftException.class)
                    .extracting(e -> ((FundingGiftException) e).getCode())
                    .isEqualTo(FundingGiftErrorCode.VOTE_LIMIT_EXCEEDED);

            verify(fundingGiftVoteRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 확정(SELECTED)된 선물이면 GIFT_ALREADY_SELECTED 예외가 발생하고 멤버 조회는 일어나지 않는다")
        void toggleVote_fail_giftAlreadySelected() {
            FundingGift selectedGift = candidateGift();
            selectedGift.select();

            given(fundingGiftRepository.findById(GIFT_ID)).willReturn(Optional.of(selectedGift));

            assertThatThrownBy(() -> fundingGiftService.toggleVote(USER_ID, FUNDING_ID, GIFT_ID))
                    .isInstanceOf(FundingGiftException.class)
                    .extracting(e -> ((FundingGiftException) e).getCode())
                    .isEqualTo(FundingGiftErrorCode.GIFT_ALREADY_SELECTED);

            verify(fundingMemberRepository, never()).findByFundingIdAndUserIdForUpdate(any(), any());
        }

        @Test
        @DisplayName("펀딩 멤버가 아니면 NOT_FUNDING_MEMBER 예외가 발생한다")
        void toggleVote_fail_notFundingMember() {
            given(fundingGiftRepository.findById(GIFT_ID)).willReturn(Optional.of(candidateGift()));
            given(fundingMemberRepository.findByFundingIdAndUserIdForUpdate(FUNDING_ID, USER_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> fundingGiftService.toggleVote(USER_ID, FUNDING_ID, GIFT_ID))
                    .isInstanceOf(FundingGiftException.class)
                    .extracting(e -> ((FundingGiftException) e).getCode())
                    .isEqualTo(FundingGiftErrorCode.NOT_FUNDING_MEMBER);
        }

        @Test
        @DisplayName("존재하지 않는 선물이면 FUNDING_GIFT_NOT_FOUND 예외가 발생한다")
        void toggleVote_fail_giftNotFound() {
            given(fundingGiftRepository.findById(GIFT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> fundingGiftService.toggleVote(USER_ID, FUNDING_ID, GIFT_ID))
                    .isInstanceOf(FundingGiftException.class)
                    .extracting(e -> ((FundingGiftException) e).getCode())
                    .isEqualTo(FundingGiftErrorCode.FUNDING_GIFT_NOT_FOUND);
        }

        @Test
        @DisplayName("다른 펀딩 소속의 선물이면 FUNDING_GIFT_NOT_FOUND 예외가 발생한다")
        void toggleVote_fail_giftBelongsToOtherFunding() {
            FundingGift gift = FundingGift.createCandidate(
                    999L, 1L, "선물", 10000L, "url", "img", null);
            ReflectionTestUtils.setField(gift, "id", GIFT_ID);

            given(fundingGiftRepository.findById(GIFT_ID)).willReturn(Optional.of(gift));

            assertThatThrownBy(() -> fundingGiftService.toggleVote(USER_ID, FUNDING_ID, GIFT_ID))
                    .isInstanceOf(FundingGiftException.class)
                    .extracting(e -> ((FundingGiftException) e).getCode())
                    .isEqualTo(FundingGiftErrorCode.FUNDING_GIFT_NOT_FOUND);
        }
    }
}
