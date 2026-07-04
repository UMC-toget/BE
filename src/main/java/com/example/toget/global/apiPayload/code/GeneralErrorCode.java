package com.example.toget.global.apiPayload.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GeneralErrorCode implements BaseErrorCode {

    // 400 Bad Request
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400_1", "잘못된 요청입니다."),
    METHOD_ARGUMENT_NOT_VALID(HttpStatus.BAD_REQUEST, "COMMON400_2", "입력값 검증에 실패하였습니다."),
    METHOD_ARGUMENT_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "COMMON400_3", "요청 파라미터의 타입이 올바르지 않습니다."),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "COMMON400_4", "필수 요청 파라미터가 누락되었습니다."),

    // 401 Unauthorized & 403 Forbidden
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON401_1", "인증되지 않았습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403_1", "접근이 금지되었습니다."),

    // 404 Not Found
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404_1", "해당 리소스를 찾을 수 없습니다."),

    // 405 Method Not Allowed
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON405_1", "지원하지 않는 HTTP 메소드입니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500_1", "서버 내부 오류가 발생했습니다. 관리자에게 문의하세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
