package com.example.toget.domain.workspace.exception;

import com.example.toget.domain.workspace.exception.code.WorkspaceErrorCode;
import com.example.toget.global.apiPayload.exception.ProjectException;

/**
 * Workspace 도메인 예외.
 * ProjectException을 상속하므로 GeneralExceptionAdvice가 알아서 에러코드에 맞춰 변환해 준다.
 */
public class WorkspaceException extends ProjectException {

    public WorkspaceException(WorkspaceErrorCode code) {
        super(code);
    }
}
