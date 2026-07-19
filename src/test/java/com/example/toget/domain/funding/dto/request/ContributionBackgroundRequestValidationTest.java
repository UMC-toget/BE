package com.example.toget.domain.funding.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ContributionBackgroundRequest Bean Validation")
class ContributionBackgroundRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("유효한 요청은 위반 사항이 없다")
    void valid_request() {
        ContributionBackgroundRequest request = new ContributionBackgroundRequest("파스텔 핑크", "#FFB6C1");

        Set<ConstraintViolation<ContributionBackgroundRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("이름이 공백이면 위반된다")
    void blank_name() {
        ContributionBackgroundRequest request = new ContributionBackgroundRequest("", "#FFB6C1");

        Set<ConstraintViolation<ContributionBackgroundRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("name");
    }

    @Test
    @DisplayName("이름이 50자를 초과하면 위반된다")
    void name_tooLong() {
        String longName = "가".repeat(51);
        ContributionBackgroundRequest request = new ContributionBackgroundRequest(longName, "#FFB6C1");

        Set<ConstraintViolation<ContributionBackgroundRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("name");
    }

    @Test
    @DisplayName("이름이 정확히 50자면 위반되지 않는다 (경계값)")
    void name_exactlyMaxLength() {
        String name50 = "가".repeat(50);
        ContributionBackgroundRequest request = new ContributionBackgroundRequest(name50, "#FFB6C1");

        Set<ConstraintViolation<ContributionBackgroundRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("HEX 코드 형식이 틀리면 위반된다")
    void invalid_hexCode() {
        ContributionBackgroundRequest request = new ContributionBackgroundRequest("파스텔 핑크", "FFB6C1");

        Set<ConstraintViolation<ContributionBackgroundRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("hexCode");
    }
}