package com.example.toget.domain.image.dto;

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

@DisplayName("PresignedUrlRequest Bean Validation")
class PresignedUrlRequestValidationTest {

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
        PresignedUrlRequest request = new PresignedUrlRequest("profiles", "avatar.png", "image/png");

        Set<ConstraintViolation<PresignedUrlRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("prefix가 null이어도 유효하다")
    void null_prefix() {
        PresignedUrlRequest request = new PresignedUrlRequest(null, "avatar.png", "image/png");

        Set<ConstraintViolation<PresignedUrlRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("prefix가 255자를 초과하면 위반된다")
    void prefix_tooLong() {
        String longPrefix = "a".repeat(256);
        PresignedUrlRequest request = new PresignedUrlRequest(longPrefix, "avatar.png", "image/png");

        Set<ConstraintViolation<PresignedUrlRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("prefix");
    }

    @Test
    @DisplayName("fileName이 255자를 초과하면 위반된다")
    void fileName_tooLong() {
        String longFileName = "a".repeat(252) + ".png";
        PresignedUrlRequest request = new PresignedUrlRequest("profiles", longFileName, "image/png");

        Set<ConstraintViolation<PresignedUrlRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("fileName");
    }

    @Test
    @DisplayName("fileName이 정확히 255자면 위반되지 않는다 (경계값)")
    void fileName_exactlyMaxLength() {
        String fileName255 = "a".repeat(251) + ".png";
        PresignedUrlRequest request = new PresignedUrlRequest("profiles", fileName255, "image/png");

        Set<ConstraintViolation<PresignedUrlRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}
