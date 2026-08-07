package com.example.toget.domain.gift.dto.request;

import com.example.toget.domain.gift.enums.WishlistType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Wishlist DTO purchaseUrl 선택 입력 검증 테스트")
class WishlistPurchaseUrlOptionalTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("WishlistCreateRequest - purchaseUrl이 null이어도 검증 통과")
    void createRequest_nullPurchaseUrl_passesValidation() {
        WishlistCreateRequest request = new WishlistCreateRequest(
                1L, "맥북 프로 14", 2490000L, null, "https://image.com/macbook.png", WishlistType.RECEIVE
        );

        Set<ConstraintViolation<WishlistCreateRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("WishlistUpdateRequest - purchaseUrl이 null이어도 검증 통과")
    void updateRequest_nullPurchaseUrl_passesValidation() {
        WishlistUpdateRequest request = new WishlistUpdateRequest(
                "맥북 프로 14", 2490000L, null, "https://image.com/macbook.png", WishlistType.RECEIVE
        );

        Set<ConstraintViolation<WishlistUpdateRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }
}
