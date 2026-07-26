package com.example.toget.global.apiPayload.handler;

import com.example.toget.global.apiPayload.ApiResponse;
import com.example.toget.global.apiPayload.code.BaseErrorCode;
import com.example.toget.global.apiPayload.code.GeneralErrorCode;
import com.example.toget.global.apiPayload.exception.ProjectException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GeneralExceptionAdvice {

    // 404 Not Found — 존재하지 않는 리소스/경로 요청 시 500이 아닌 404 (COMMON404_1) 응답
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(
            NoResourceFoundException e
    ) {
        log.warn("NoResourceFoundException: ", e);
        BaseErrorCode code = GeneralErrorCode.NOT_FOUND;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.onFailure(code, null));
    }


    // 프로젝트에서 발생한 예외 처리
    @ExceptionHandler(com.example.toget.global.apiPayload.exception.ProjectException.class)
    public ResponseEntity<ApiResponse<Void>> handleMemberException(
            ProjectException e
    ) {
        BaseErrorCode errorCode = e.getCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.onFailure(errorCode, null));
    }

    // DB 유니크 제약 위반 등 데이터 충돌 — 동시 중복 요청(예: 소셜 로그인 동시 첫 가입)을 500이 아닌 409로 응답
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException e
    ) {
        BaseErrorCode code = GeneralErrorCode.CONFLICT;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.onFailure(code, null));
    }

    // 그 외의 정의되지 않은 모든 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception ex
    ) {
        log.error("Internal Server Error: ", ex);
        BaseErrorCode code = GeneralErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.onFailure(code, null));
    }

    // HttpMessageNotReadableException (빈 바디, 잘못된 JSON 가공 등) 처리
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e
    ) {
        log.warn("HttpMessageNotReadableException: ", e);
        BaseErrorCode code = GeneralErrorCode.BAD_REQUEST;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.onFailure(code, null));
    }

    // MethodArgumentTypeMismatchException (타입 미스매칭) 처리
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e
    ) {
        log.warn("MethodArgumentTypeMismatchException: ", e);
        BaseErrorCode code = GeneralErrorCode.METHOD_ARGUMENT_TYPE_MISMATCH;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.onFailure(code, null));
    }

    // MissingServletRequestParameterException (필수 파라미터 누락) 처리
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e
    ) {
        log.warn("MissingServletRequestParameterException: ", e);
        BaseErrorCode code = GeneralErrorCode.MISSING_PARAMETER;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.onFailure(code, null));
    }

    // HttpRequestMethodNotSupportedException (지원하지 않는 HTTP 메소드) 처리
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e
    ) {
        log.warn("HttpRequestMethodNotSupportedException: ", e);
        BaseErrorCode code = GeneralErrorCode.METHOD_NOT_ALLOWED;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.onFailure(code, null));
    }

    // @Valid 어노테이션 검증 실패 예외
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e
    ){
        // 검증 실패한 변수명과 실패 이유를 담을 Map
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });

        BaseErrorCode code = GeneralErrorCode.BAD_REQUEST;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.onFailure(code, errors));
    }
}
