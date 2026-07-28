package com.example.toget.domain.funding.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FundingVisibilityUpdateRequest Bean Validation 테스트.
 *
 * [설계 포인트]
 *  - @Valid는 컨트롤러 경계에서 Spring MVC가 수행하므로, FundingServiceTest(서비스 단위 테스트)로는
 *    @NotNull이 붙어 있는지 검증할 수 없다. 애노테이션이 지워져도 서비스 테스트는 전부 통과한다.
 *  - 검증이 빠지면 null이 그대로 엔티티까지 내려가 NOT NULL 컬럼 위반이 되고,
 *    "필수 필드 누락"이 400이 아니라 409(DataIntegrityViolationException)로 응답된다.
 *    그 상황을 막기 위해 필드별 @NotNull 존재를 이 테스트가 고정한다.
 */
@DisplayName("FundingVisibilityUpdateRequest Bean Validation")
class FundingVisibilityUpdateRequestValidationTest {

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
    @DisplayName("5개 필드를 모두 채우면 위반 사항이 없다")
    void valid_request() {
        FundingVisibilityUpdateRequest request =
                new FundingVisibilityUpdateRequest(true, false, true, false, true);

        Set<ConstraintViolation<FundingVisibilityUpdateRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    /**
     * PUT(전체 교체)이라 일부만 보내는 것을 허용하지 않는다.
     * 필드를 하나씩 null로 만들어, 다섯 개 모두 @NotNull이 걸려 있는지 확인한다.
     */
    @ParameterizedTest(name = "{0}이(가) null이면 위반된다")
    @CsvSource({
            "showProgress,          , false, true,  false, true",
            "showAmount,        true,      , true,  false, true",
            "showParticipantCount,  true, false,      , false, true",
            "showParticipantNames,  true, false, true,      , true",
            "showMessages,          true, false, true,  false,     "
    })
    @DisplayName("필드가 누락되면 해당 필드에 위반이 발생한다")
    void null_field_violates(String fieldName, Boolean showProgress, Boolean showAmount,
                             Boolean showParticipantCount, Boolean showParticipantNames,
                             Boolean showMessages) {
        FundingVisibilityUpdateRequest request = new FundingVisibilityUpdateRequest(
                showProgress, showAmount, showParticipantCount, showParticipantNames, showMessages
        );

        Set<ConstraintViolation<FundingVisibilityUpdateRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo(fieldName);
    }

    @Test
    @DisplayName("전부 null이면 5개 필드 모두 위반된다")
    void all_null_violates_every_field() {
        FundingVisibilityUpdateRequest request =
                new FundingVisibilityUpdateRequest(null, null, null, null, null);

        Set<ConstraintViolation<FundingVisibilityUpdateRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(5);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder(
                        "showProgress", "showAmount", "showParticipantCount",
                        "showParticipantNames", "showMessages"
                );
    }
}
