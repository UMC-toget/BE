package com.example.toget.domain.gift.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * giftPrice(Long)에 CharSequence 전용 제약인 @NotBlank가 잘못 붙어 있으면
 * validator.validate() 자체가 UnexpectedTypeException을 던져 요청 값과 무관하게 500이 나던 회귀 방지 테스트 (issue #107).
 */
@DisplayName("FundingGiftCandidateCreateRequest 검증 테스트")
class FundingGiftCandidateCreateRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("정상 값이면 검증기가 예외 없이 통과시킨다")
    void validRequest_passesValidation() {
        FundingGiftCandidateCreateRequest request = new FundingGiftCandidateCreateRequest(
                "https://image.com/ps5.png", "플레이스테이션 5 Slim", 598000L, "다들 이거 갖고 싶어해요",
                "https://store.com/ps5"
        );

        Set<ConstraintViolation<FundingGiftCandidateCreateRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("빈 바디({})로 giftPrice가 null이어도 UnexpectedTypeException 없이 필수값 위반만 보고된다")
    void nullGiftPrice_doesNotThrowUnexpectedTypeException() {
        FundingGiftCandidateCreateRequest request = new FundingGiftCandidateCreateRequest(
                null, null, null, null, null
        );

        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();

        Set<ConstraintViolation<FundingGiftCandidateCreateRequest>> violations = validator.validate(request);
        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .contains("giftName", "giftPrice", "note");
    }

    @Test
    @DisplayName("giftPrice가 0 이하면 @Positive 위반이 보고된다")
    void nonPositiveGiftPrice_violatesPositive() {
        FundingGiftCandidateCreateRequest request = new FundingGiftCandidateCreateRequest(
                null, "선물", 0L, "메모", null
        );

        Set<ConstraintViolation<FundingGiftCandidateCreateRequest>> violations = validator.validate(request);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("giftPrice");
    }
}
