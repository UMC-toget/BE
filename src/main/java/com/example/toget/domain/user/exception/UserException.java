package com.example.toget.domain.user.exception;

import com.example.toget.global.apiPayload.exception.ProjectException;

/**
 * User 도메인 예외.
 * ProjectException을 상속하므로 어디서 던져도 GeneralExceptionAdvice(@RestControllerAdvice)가
 * 잡아서 에러코드에 맞는 HTTP 상태 + ApiResponse JSON으로 변환해 준다.
 * → 서비스 코드는 try-catch 없이 "던지기만" 하면 된다.
 */
public class UserException extends ProjectException {

    public UserException(UserErrorCode code) {
        super(code);
    }
}
