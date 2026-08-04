package com.example.toget.domain.bank.exception.code;

import com.example.toget.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Bank 도메인 성공코드 카탈로그.
 * 코드 규칙: 리소스명 + HTTP상태 + _순번 (에러코드 BankErrorCode와 대칭)
 */
@Getter
@RequiredArgsConstructor
public enum BankSuccessCode implements BaseSuccessCode {

    BANK_LIST_OK(HttpStatus.OK, "BANK200_1", "은행 목록을 성공적으로 조회했습니다."),
    BANK_UPDATE_OK(HttpStatus.OK, "BANK200_2", "은행 정보를 성공적으로 수정했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
