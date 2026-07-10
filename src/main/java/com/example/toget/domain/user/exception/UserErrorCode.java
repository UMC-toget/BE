package com.example.toget.domain.user.exception;

import com.example.toget.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * User 도메인 에러코드 카탈로그.
 * 각 상수가 (HTTP 상태, 클라이언트용 코드 문자열, 메시지) 세트를 들고 있어서
 * UserException에 담아 던지면 GeneralExceptionAdvice가 이 정보 그대로 응답을 만든다.
 * 코드 규칙: USER + HTTP상태 + _순번 (예: USER401_1)
 */
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements BaseErrorCode {

    // 401 Unauthorized — 인증 정보 없음/무효, 토큰 만료·위조, refresh 토큰 재사용 감지 등
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "USER401_1", "인증 정보가 올바르지 않습니다."),

    // 400 Bad Request — 지원하지 않는 소셜 로그인 공급자
    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "USER400_1", "지원하지 않는 소셜 로그인 공급자입니다."),

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER404_1", "존재하지 않는 사용자입니다."),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "USER404_2", "존재하지 않는 계좌입니다."),

    // 403 Forbidden — 본인 소유가 아닌 계좌에 대한 접근
    ACCOUNT_FORBIDDEN(HttpStatus.FORBIDDEN, "USER403_1", "해당 계좌에 대한 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
