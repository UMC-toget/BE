package com.example.toget.domain.funding.service;

import com.example.toget.domain.funding.dto.request.FundingReviewCreateRequest;
import com.example.toget.domain.funding.dto.request.FundingReviewTitledCreateRequest;
import com.example.toget.domain.funding.dto.response.FundingReviewCreateResponse;
import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.entity.FundingReview;
import com.example.toget.domain.funding.enums.FundingReviewType;
import com.example.toget.domain.funding.exception.FundingException;
import com.example.toget.domain.funding.exception.code.FundingErrorCode;
import com.example.toget.domain.funding.repository.FundingRepository;
import com.example.toget.domain.funding.repository.FundingReviewImageRepository;
import com.example.toget.domain.funding.repository.FundingReviewRepository;
import com.example.toget.domain.funding.repository.ContributionBackgroundRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("FundingReviewService")
class FundingReviewServiceTest {

    @Mock
    private FundingRepository fundingRepository;
    @Mock
    private FundingReviewRepository fundingReviewRepository;
    @Mock
    private FundingReviewImageRepository fundingReviewImageRepository;
    @Mock
    private ContributionBackgroundRepository contributionBackgroundRepository;

    @InjectMocks
    private FundingReviewService fundingReviewService;

    @Nested
    @DisplayName("선물 후기 작성")
    class CreateReview {

        @Test
        @DisplayName("TOGETHER_GIFT 펀딩에 요청하면 NOT_MY_GIFT_TYPE 예외가 발생한다")
        void createReview_fail_notMyGiftType() {
            Long userId = 1L;
            Long fundingId = 10L;
            Funding funding = Funding.createTogetherGift(userId, null, "제목", "홍길동",
                    LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(30),
                    "소개", "url", 0L);
            ReflectionTestUtils.setField(funding, "id", fundingId);

            given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));

            FundingReviewCreateRequest request = new FundingReviewCreateRequest(
                    "내용", 1L, null, null, null, 2L, 3L
            );

            assertThatThrownBy(() -> fundingReviewService.createReview(userId, fundingId, request))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.NOT_MY_GIFT_TYPE);
        }

        @Test
        @DisplayName("이미 후기가 작성된 펀딩이면 REVIEW_ALREADY_EXISTS 예외가 발생한다")
        void createReview_fail_alreadyExists() {
            Long userId = 1L;
            Long fundingId = 10L;
            Funding funding = Funding.createMyGift(userId, 5L, "제목", "홍길동",
                    LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(30),
                    "소개", "url", 100000L);
            ReflectionTestUtils.setField(funding, "id", fundingId);

            given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
            given(fundingReviewRepository.existsByFundingIdAndType(fundingId, FundingReviewType.REVIEW))
                    .willReturn(true);

            FundingReviewCreateRequest request = new FundingReviewCreateRequest(
                    "내용", 1L, null, null, null, 2L, 3L
            );

            assertThatThrownBy(() -> fundingReviewService.createReview(userId, fundingId, request))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.REVIEW_ALREADY_EXISTS);
        }

        @Test
        @DisplayName("개설자가 아니면 NOT_FUNDING_OWNER 예외가 발생한다")
        void createReview_fail_notOwner() {
            Long ownerId = 1L;
            Long requesterId = 2L;
            Long fundingId = 10L;
            Funding funding = Funding.createMyGift(ownerId, 5L, "제목", "홍길동",
                    LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(30),
                    "소개", "url", 100000L);
            ReflectionTestUtils.setField(funding, "id", fundingId);

            given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));

            FundingReviewCreateRequest request = new FundingReviewCreateRequest(
                    "내용", 1L, null, null, null, 2L, 3L
            );

            assertThatThrownBy(() -> fundingReviewService.createReview(requesterId, fundingId, request))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.NOT_FUNDING_OWNER);
        }
    }

    @Nested
    @DisplayName("전달 소식 작성")
    class CreateNews {

        @Test
        @DisplayName("MY_GIFT 펀딩에 요청하면 NOT_TOGETHER_GIFT_TYPE 예외가 발생한다")
        void createNews_fail_notTogetherGiftType() {
            Long userId = 1L;
            Long fundingId = 10L;
            Funding funding = Funding.createMyGift(userId, 5L, "제목", "홍길동",
                    LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(30),
                    "소개", "url", 100000L);
            ReflectionTestUtils.setField(funding, "id", fundingId);

            given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));

            FundingReviewTitledCreateRequest request = new FundingReviewTitledCreateRequest(
                    "제목", "내용", null, null, null, 2L, 3L
            );

            assertThatThrownBy(() -> fundingReviewService.createNews(userId, fundingId, request))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.NOT_TOGETHER_GIFT_TYPE);
        }
    }

    @Nested
    @DisplayName("게시물 조회")
    class GetReview {

        @Test
        @DisplayName("존재하지 않는 type 문자열이면 REVIEW_NOT_FOUND 예외가 발생한다")
        void getReview_fail_invalidType() {
            Long fundingId = 10L;


            assertThatThrownBy(() -> fundingReviewService.getReview(fundingId, "invalid-type"))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.REVIEW_NOT_FOUND);
        }

        @Test
        @DisplayName("소문자 type도 정상적으로 파싱된다")
        void getReview_success_lowercaseType() {
            Long fundingId = 10L;
            Funding funding = Funding.createMyGift(1L, 5L, "제목", "홍길동",
                    LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(30),
                    "소개", "url", 100000L);
            given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
            given(fundingReviewRepository.findByFundingIdAndType(fundingId, FundingReviewType.REVIEW))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> fundingReviewService.getReview(fundingId, "review"))
                    .isInstanceOf(FundingException.class)
                    .extracting(e -> ((FundingException) e).getCode())
                    .isEqualTo(FundingErrorCode.REVIEW_NOT_FOUND);  // 파싱은 성공, 데이터가 없어서 나는 예외
        }
    }
}