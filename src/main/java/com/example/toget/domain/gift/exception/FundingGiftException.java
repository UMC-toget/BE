package com.example.toget.domain.gift.exception;


import com.example.toget.domain.funding.exception.code.FundingErrorCode;
import com.example.toget.domain.gift.exception.code.FundingGiftErrorCode;
import com.example.toget.global.apiPayload.exception.ProjectException;

public class FundingGiftException extends ProjectException {
    public FundingGiftException(FundingGiftErrorCode code) {
        super(code);
    }
}