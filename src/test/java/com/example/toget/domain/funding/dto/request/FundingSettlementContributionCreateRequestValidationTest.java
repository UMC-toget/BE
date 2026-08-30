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

/**
 * FundingSettlementContributionCreateRequest Bean Validation 테스트.
 *
 * [설계 포인트]
 *  - 이 엔드포인트(POST /fundings/{fundingId}/members/me/contributions)는 로그인 멤버 전용이라
 *    비인증 도배 위험은 없지만, FE 편지 작성 UI가 234자(공백 포함)로 입력을 막고 있어
 *    서버도 동일하게 막아야 클라이언트 검증 우회를 방어할 수 있다 (issue #129).
 *  - @Valid는 컨트롤러 경계에서 Spring MVC가 수행하므로, 서비스 단위 테스트로는
 *    @Size가 붙어 있는지 검증할 수 없다. 애노테이션이 지워져도 서비스 테스트는 통과한다.
 */
@DisplayName("FundingSettlementContributionCreateRequest Bean Validation")
class FundingSettlementContributionCreateRequestValidationTest {

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
    @DisplayName("content가 234자를 초과하면 위반된다")
    void content_exceeds_max_length() {
        String tooLong = "가".repeat(235);
        FundingSettlementContributionCreateRequest request =
                new FundingSettlementContributionCreateRequest(1L, tooLong, false);

        Set<ConstraintViolation<FundingSettlementContributionCreateRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("content");
    }

    @Test
    @DisplayName("content가 정확히 234자면 위반되지 않는다 — FE 편지 작성 UI 글자수 제한과 동일 (issue #129)")
    void content_at_max_length_is_valid() {
        String exactly234 = "가".repeat(234);
        FundingSettlementContributionCreateRequest request =
                new FundingSettlementContributionCreateRequest(1L, exactly234, false);

        Set<ConstraintViolation<FundingSettlementContributionCreateRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("content가 null이어도 위반되지 않는다 — 편지 없이 정산만 신고하는 경우 허용")
    void content_null_is_valid() {
        FundingSettlementContributionCreateRequest request =
                new FundingSettlementContributionCreateRequest(1L, null, false);

        Set<ConstraintViolation<FundingSettlementContributionCreateRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("필수 필드를 모두 채우고 길이 제한을 지키면 위반 사항이 없다")
    void valid_request() {
        FundingSettlementContributionCreateRequest request =
                new FundingSettlementContributionCreateRequest(1L, "길동아 진심으로 축하해! 나도 함께할게", false);

        Set<ConstraintViolation<FundingSettlementContributionCreateRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}
