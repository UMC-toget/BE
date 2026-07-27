package com.example.toget.domain.gift.exception;

import com.example.toget.domain.gift.exception.code.WishlistErrorCode;
import com.example.toget.global.apiPayload.exception.ProjectException;

public class WishlistException extends ProjectException {
    public WishlistException(WishlistErrorCode code) {
        super(code);
    }
}
