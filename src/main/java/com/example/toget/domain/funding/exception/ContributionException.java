package com.example.toget.domain.funding.exception;

import com.example.toget.domain.funding.exception.code.ContributionErrorCode;
import com.example.toget.global.apiPayload.exception.ProjectException;

public class ContributionException extends ProjectException {
    public ContributionException(ContributionErrorCode code) {
        super(code);
    }
}