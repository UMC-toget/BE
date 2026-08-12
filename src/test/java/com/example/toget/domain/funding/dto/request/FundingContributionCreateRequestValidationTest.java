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
 * FundingContributionCreateRequest Bean Validation 테스트.
 *
 * [설계 포인트]
 *  - 이 엔드포인트(POST /fundings/{fundingId}/contributions)는 비회원도 호출 가능한
 *    완전 비인증 공개 API라, senderName/content에 길이 제한이 없으면 대용량 문자열을
 *    무제한 저장할 수 있는 도배 벡터가 된다 (issue #84).
 *  - @Valid는 컨트롤러 경계에서 Spring MVC가 수행하므로, 서비스 단위 테스트로는
 *    @Size가 붙어 있는지 검증할 수 없다. 애노테이션이 지워져도 서비스 테스트는 통과한다.
 */
@DisplayName("FundingContributionCreateRequest Bean Validation")
class FundingContributionCreateRequestValidationTest {

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
    @DisplayName("senderName이 50자를 초과하면 위반된다")
    void senderName_exceeds_max_length() {
        String tooLong = "가".repeat(51);
        FundingContributionCreateRequest request = new FundingContributionCreateRequest(
                tooLong, 1L, false, 0L, "축하해!", false
        );

        Set<ConstraintViolation<FundingContributionCreateRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("senderName");
    }

    @Test
    @DisplayName("senderName이 정확히 50자면 위반되지 않는다")
    void senderName_at_max_length_is_valid() {
        String exactly50 = "가".repeat(50);
        FundingContributionCreateRequest request = new FundingContributionCreateRequest(
                exactly50, 1L, false, 0L, "축하해!", false
        );

        Set<ConstraintViolation<FundingContributionCreateRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("content가 234자를 초과하면 위반된다")
    void content_exceeds_max_length() {
        String tooLong = "가".repeat(235);
        FundingContributionCreateRequest request = new FundingContributionCreateRequest(
                "테스트", 1L, false, 0L, tooLong, false
        );

        Set<ConstraintViolation<FundingContributionCreateRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("content");
    }

    @Test
    @DisplayName("content가 정확히 234자면 위반되지 않는다 — FE 편지 작성 UI 글자수 제한과 동일 (issue #129)")
    void content_at_max_length_is_valid() {
        String exactly234 = "가".repeat(234);
        FundingContributionCreateRequest request = new FundingContributionCreateRequest(
                "테스트", 1L, false, 0L, exactly234, false
        );

        Set<ConstraintViolation<FundingContributionCreateRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("필수 필드를 모두 채우고 길이 제한을 지키면 위반 사항이 없다")
    void valid_request() {
        FundingContributionCreateRequest request = new FundingContributionCreateRequest(
                "축하하는 친구", 1L, false, 50000L, "길동아 진심으로 생일 축하해!", false
        );

        Set<ConstraintViolation<FundingContributionCreateRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}
