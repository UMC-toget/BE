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
import com.example.toget.domain.funding.dto.response.FundingReviewDetailResponse;
import com.example.toget.domain.user.entity.User;
import com.example.toget.domain.user.enums.OAuthProvider;
import com.example.toget.domain.user.repository.UserRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FundingReviewService fundingReviewService;

    private User userWithNames(Long id, String name, String nickname) {
        User user = User.builder()
                .oAuthProvider(OAuthProvider.GOOGLE)
                .oAuthId("oauth-" + id)
                .email("user" + id + "@toget.com")
                .name(name)
                .nickname(nickname)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

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

        /**
         * 작성자 표기(from) — issue #105.
         * REVIEW/NEWS는 개설자만 작성 가능하고 MY_GIFT는 funding_members를 안 써서, 작성자는
         * 항상 Funding.userId 한 명으로 고정된다. 그래서 회원 정보를 조회 시점에 조인해서 채운다.
         */
        @Test
        @DisplayName("REVIEW 조회 시 개설자 닉네임이 authorName으로 채워진다")
        void getReview_review_authorNameFromNickname() {
            Long ownerId = 1L;
            Long fundingId = 10L;
            Funding funding = Funding.createMyGift(ownerId, 5L, "제목", "홍길동",
                    LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(30),
                    "소개", "url", 100000L);
            FundingReview review = FundingReview.createReview(
                    fundingId, "내용", 1L, null, null, null, null
            );
            ReflectionTestUtils.setField(review, "id", 7L);

            given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
            given(fundingReviewRepository.findByFundingIdAndType(fundingId, FundingReviewType.REVIEW))
                    .willReturn(Optional.of(review));
            given(fundingReviewImageRepository.findAllByFundingReviewId(7L)).willReturn(List.of());
            given(userRepository.findById(ownerId))
                    .willReturn(Optional.of(userWithNames(ownerId, "홍길동", "김방장")));

            FundingReviewDetailResponse result = fundingReviewService.getReview(fundingId, "review");

            assertThat(result.authorName()).isEqualTo("김방장");
        }

        @Test
        @DisplayName("REVIEW 조회 시 닉네임이 비어 있으면 이름으로 대체된다")
        void getReview_review_authorNameFallsBackToName() {
            Long ownerId = 1L;
            Long fundingId = 10L;
            Funding funding = Funding.createMyGift(ownerId, 5L, "제목", "홍길동",
                    LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(30),
                    "소개", "url", 100000L);
            FundingReview review = FundingReview.createReview(
                    fundingId, "내용", 1L, null, null, null, null
            );
            ReflectionTestUtils.setField(review, "id", 7L);

            given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
            given(fundingReviewRepository.findByFundingIdAndType(fundingId, FundingReviewType.REVIEW))
                    .willReturn(Optional.of(review));
            given(fundingReviewImageRepository.findAllByFundingReviewId(7L)).willReturn(List.of());
            given(userRepository.findById(ownerId))
                    .willReturn(Optional.of(userWithNames(ownerId, "홍길동", null)));

            FundingReviewDetailResponse result = fundingReviewService.getReview(fundingId, "review");

            assertThat(result.authorName()).isEqualTo("홍길동");
        }

        @Test
        @DisplayName("NEWS(전달 소식) 조회 시에도 authorName이 채워진다")
        void getReview_news_authorNameFilled() {
            Long ownerId = 1L;
            Long fundingId = 10L;
            Funding funding = Funding.createTogetherGift(ownerId, null, "제목", "홍길동",
                    LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(30),
                    "소개", "url", 100000L);
            FundingReview review = FundingReview.createNews(
                    fundingId, "소식 제목", "내용", null, null, null, null
            );
            ReflectionTestUtils.setField(review, "id", 8L);

            given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
            given(fundingReviewRepository.findByFundingIdAndType(fundingId, FundingReviewType.NEWS))
                    .willReturn(Optional.of(review));
            given(fundingReviewImageRepository.findAllByFundingReviewId(8L)).willReturn(List.of());
            given(userRepository.findById(ownerId))
                    .willReturn(Optional.of(userWithNames(ownerId, "홍길동", "김방장")));

            FundingReviewDetailResponse result = fundingReviewService.getReview(fundingId, "news");

            assertThat(result.authorName()).isEqualTo("김방장");
        }

        @Test
        @DisplayName("HEARTFELT(마음전하기) 조회는 authorName이 null이고 회원 조회 자체를 하지 않는다")
        void getReview_heartfelt_authorNameIsNull() {
            Long ownerId = 1L;
            Long fundingId = 10L;
            Funding funding = Funding.createTogetherGift(ownerId, null, "제목", "홍길동",
                    LocalDate.now(), LocalDate.now(), LocalDate.now().plusDays(30),
                    "소개", "url", 100000L);
            FundingReview review = FundingReview.createHeartfelt(
                    fundingId, "마음 제목", "내용", null, null, null, null
            );
            ReflectionTestUtils.setField(review, "id", 9L);

            given(fundingRepository.findById(fundingId)).willReturn(Optional.of(funding));
            given(fundingReviewRepository.findByFundingIdAndType(fundingId, FundingReviewType.HEARTFELT))
                    .willReturn(Optional.of(review));
            given(fundingReviewImageRepository.findAllByFundingReviewId(9L)).willReturn(List.of());

            FundingReviewDetailResponse result = fundingReviewService.getReview(fundingId, "heartfelt");

            assertThat(result.authorName()).isNull();
            verify(userRepository, never()).findById(any());
        }
    }
}