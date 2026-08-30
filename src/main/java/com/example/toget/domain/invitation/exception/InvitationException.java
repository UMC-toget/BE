package com.example.toget.domain.invitation.exception;

import com.example.toget.domain.invitation.exception.code.InvitationErrorCode;
import com.example.toget.global.apiPayload.exception.ProjectException;

/**
 * Invitation 도메인 예외.
 * ProjectException을 상속하므로 어디서 던져도 GeneralExceptionAdvice가
 * 에러코드에 맞는 HTTP 상태 + ApiResponse JSON으로 변환한다.
 */
public class InvitationException extends ProjectException {

    public InvitationException(InvitationErrorCode code) {
        super(code);
    }
}
