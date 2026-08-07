package com.example.toget.domain.funding.entity;

import com.example.toget.domain.funding.exception.code.FundingErrorCode;
import com.example.toget.global.apiPayload.exception.ProjectException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Funding 엔티티")
class FundingTest {

    private static final Long USER_ID = 1L;
    private static final Long ACCOUNT_ID = 5L;
    private static final String TITLE = "제목";
    private static final String RECIPIENT_NAME = "홍길동";
    private static final LocalDate ANNIVERSARY = LocalDate.of(2026, 8, 15);

    @Nested
    @DisplayName("createMyGift 생성 시")
    class CreateMyGift {

        @Test
        @DisplayName("시작일/종료일이 있으면 정상 생성된다")
        void create_success() {
            // when
            Funding funding = Funding.createMyGift(USER_ID, ACCOUNT_ID, TITLE, RECIPIENT_NAME, ANNIVERSARY,
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                    null, null, 100_000L);

            // then
            assertThat(funding.getStartDate()).isEqualTo(LocalDate.of(2026, 7, 1));
            assertThat(funding.getEndDate()).isEqualTo(LocalDate.of(2026, 7, 31));
        }

        @Test
        @DisplayName("시작일이 없으면 예외가 발생한다 — MY_GIFT는 기간이 필수")
        void create_fail_whenStartDateIsNull() {
            assertThatThrownBy(() -> Funding.createMyGift(USER_ID, ACCOUNT_ID, TITLE, RECIPIENT_NAME, ANNIVERSARY,
                    null, LocalDate.of(2026, 7, 31),
                    null, null, 100_000L))
                    .isInstanceOf(ProjectException.class)
                    .extracting(e -> ((ProjectException) e).getCode())
                    .isEqualTo(FundingErrorCode.PERIOD_REQUIRED_FOR_MY_GIFT);
        }

        @Test
        @DisplayName("종료일이 없으면 예외가 발생한다 — MY_GIFT는 기간이 필수")
        void create_fail_whenEndDateIsNull() {
            assertThatThrownBy(() -> Funding.createMyGift(USER_ID, ACCOUNT_ID, TITLE, RECIPIENT_NAME, ANNIVERSARY,
                    LocalDate.of(2026, 7, 1), null,
                    null, null, 100_000L))
                    .isInstanceOf(ProjectException.class)
                    .extracting(e -> ((ProjectException) e).getCode())
                    .isEqualTo(FundingErrorCode.PERIOD_REQUIRED_FOR_MY_GIFT);
        }
    }

    @Nested
    @DisplayName("createTogetherGift 생성 시")
    class CreateTogetherGift {

        @Test
        @DisplayName("시작일/종료일이 둘 다 없어도 정상 생성된다 — TOGETHER_GIFT는 기간 선택")
        void create_success_whenPeriodIsNull() {
            // when
            Funding funding = Funding.createTogetherGift(USER_ID, null, TITLE, RECIPIENT_NAME, ANNIVERSARY,
                    null, null, null, null, 0L);

            // then
            assertThat(funding.getStartDate()).isNull();
            assertThat(funding.getEndDate()).isNull();
        }

        @Test
        @DisplayName("시작일만 있고 종료일이 없으면 예외가 발생한다 — 불완전한 기간은 허용하지 않는다")
        void create_fail_whenOnlyStartDateIsGiven() {
            assertThatThrownBy(() -> Funding.createTogetherGift(USER_ID, null, TITLE, RECIPIENT_NAME, ANNIVERSARY,
                    LocalDate.of(2026, 7, 1), null, null, null, 0L))
                    .isInstanceOf(ProjectException.class)
                    .extracting(e -> ((ProjectException) e).getCode())
                    .isEqualTo(FundingErrorCode.INVALID_FUNDING_PERIOD);
        }

        @Test
        @DisplayName("종료일이 시작일보다 앞서면 예외가 발생한다")
        void create_fail_whenEndDateBeforeStartDate() {
            assertThatThrownBy(() -> Funding.createTogetherGift(USER_ID, null, TITLE, RECIPIENT_NAME, ANNIVERSARY,
                    LocalDate.of(2026, 7, 31), LocalDate.of(2026, 7, 1), null, null, 0L))
                    .isInstanceOf(ProjectException.class)
                    .extracting(e -> ((ProjectException) e).getCode())
                    .isEqualTo(FundingErrorCode.INVALID_FUNDING_PERIOD);
        }

        @Test
        @DisplayName("기간 미설정 상태(endDate=null)에서는 needsEndDecision이 NPE 없이 false를 반환한다")
        void needsEndDecision_falseWhenEndDateIsNull() {
            // given
            Funding funding = Funding.createTogetherGift(USER_ID, null, TITLE, RECIPIENT_NAME, ANNIVERSARY,
                    null, null, null, null, 0L);

            // when & then — SELECTING 초기 상태 + endDate null
            assertThat(funding.needsEndDecision()).isFalse();
        }
    }
}
