package com.example.toget.domain.funding.exception.code;

import com.example.toget.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ContributionErrorCode implements BaseErrorCode {

    BACKGROUND_NOT_FOUND(HttpStatus.NOT_FOUND, "METADATA404", "해당 배경 색상을 찾을 수 없습니다."),
    INVALID_BACKGROUND_NAME(HttpStatus.BAD_REQUEST, "METADATA400_1", "배경 색상 이름은 필수입니다."),
    INVALID_HEX_CODE(HttpStatus.BAD_REQUEST, "METADATA400_2", "HEX 코드 형식이 올바르지 않습니다. (예: #FFB6C1)");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
