package com.example.toget.domain.bank.exception;

import com.example.toget.domain.bank.exception.code.BankErrorCode;
import com.example.toget.global.apiPayload.exception.ProjectException;

/**
 * Bank 도메인 예외.
 * ProjectException을 상속하므로 어디서 던져도 GeneralExceptionAdvice(@RestControllerAdvice)가
 * 잡아서 에러코드에 맞는 HTTP 상태 + ApiResponse JSON으로 변환해 준다.
 */
public class BankException extends ProjectException {

    public BankException(BankErrorCode code) {
        super(code);
    }
}
