package com.example.toget.domain.invitation.dto.request;

import com.example.toget.domain.invitation.dto.InvitationBackgroundRequest;
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

@DisplayName("InvitationBackgroundRequest Bean Validation")
public class InvitationBackgroundRequestValidationTest {

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
        InvitationBackgroundRequest request = new InvitationBackgroundRequest("파스텔 옐로우", "#FFFFE0", "#FFD700");

        Set<ConstraintViolation<InvitationBackgroundRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("원색 HEX 코드 형식이 올바르지 않으면 위반된다")
    void invalid_solidColorHex() {
        InvitationBackgroundRequest request = new InvitationBackgroundRequest("파스텔 옐로우", "#FFFFE0", "FFD700");

        Set<ConstraintViolation<InvitationBackgroundRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("solidColorHex");
    }
}
