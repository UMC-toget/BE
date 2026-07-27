package com.example.toget.domain.gift.exception.code;

import com.example.toget.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WishlistErrorCode implements BaseErrorCode {

    WISHLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "WISHLIST404", "존재하지 않는 위시리스트 아이템입니다."),
    WISHLIST_NOT_OWNER(HttpStatus.FORBIDDEN, "WISHLIST403", "본인의 위시리스트 아이템만 접근할 수 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
