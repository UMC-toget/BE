package com.example.toget.global.apiPayload.exception;

import com.example.toget.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class ProjectException extends RuntimeException {
    private final BaseErrorCode code;
}
