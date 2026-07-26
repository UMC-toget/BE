package com.example.toget.domain.gift.exception;

import com.example.toget.domain.gift.exception.code.ProductErrorCode;
import com.example.toget.global.apiPayload.exception.ProjectException;

public class ProductException extends ProjectException {
    public ProductException(ProductErrorCode code) {
        super(code);
    }
}
