package com.example.toget.global.apiPayload.handler;

import com.example.toget.global.apiPayload.ApiResponse;
import com.example.toget.global.apiPayload.code.GeneralErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GeneralExceptionAdvice DataIntegrityViolationException 세분화 테스트")
class GeneralExceptionAdviceDataIntegrityTest {

    private final GeneralExceptionAdvice advice = new GeneralExceptionAdvice();

    @Test
    @DisplayName("유니크 제약 위반(duplicate key) 예외 발생 시 409 CONFLICT 반환")
    void handleDataIntegrityViolation_uniqueConstraint_returnsConflict() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException("Duplicate entry '1' for key 'uk_user_product_type'");

        ResponseEntity<ApiResponse<Void>> response = advice.handleDataIntegrityViolation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(GeneralErrorCode.CONFLICT.getCode());
    }

    @Test
    @DisplayName("외래키 제약 실패(FK failure) 예외 발생 시 500 INTERNAL_SERVER_ERROR 반환")
    void handleDataIntegrityViolation_foreignKeyConstraint_returnsInternalServerError() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException("Cannot add or update a child row: a foreign key constraint fails (`users`, CONSTRAINT `fk_user`)");

        ResponseEntity<ApiResponse<Void>> response = advice.handleDataIntegrityViolation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(GeneralErrorCode.INTERNAL_SERVER_ERROR.getCode());
    }

    @Test
    @DisplayName("NOT NULL 제약 실패(null value) 예외 발생 시 500 INTERNAL_SERVER_ERROR 반환")
    void handleDataIntegrityViolation_notNullConstraint_returnsInternalServerError() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException("Column 'user_id' cannot be null");

        ResponseEntity<ApiResponse<Void>> response = advice.handleDataIntegrityViolation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(GeneralErrorCode.INTERNAL_SERVER_ERROR.getCode());
    }
}
