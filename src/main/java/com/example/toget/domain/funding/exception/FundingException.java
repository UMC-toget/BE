package com.example.toget.domain.funding.exception;

import com.example.toget.domain.funding.exception.code.FundingErrorCode;
import com.example.toget.global.apiPayload.exception.ProjectException;

public class FundingException extends ProjectException {
    public FundingException(FundingErrorCode code) {
        super(code);
    }
}