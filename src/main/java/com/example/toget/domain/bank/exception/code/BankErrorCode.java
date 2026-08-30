package com.example.toget.domain.bank.exception.code;

import com.example.toget.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Bank 도메인 에러코드 카탈로그.
 * 코드 규칙: 리소스명 + HTTP상태 + _순번
 */
@Getter
@RequiredArgsConstructor
public enum BankErrorCode implements BaseErrorCode {

    // 404 Not Found — 관리자 수정 대상 은행이 없거나, 계좌 등록 시 요청한 은행 코드가 아직 시드되지 않은 경우.
    // 후자는 정상 상태라면 발생하지 않는다(시더가 BankName 전체를 채우므로).
    // 실제로 뜬다면 시더 미실행을 의심해야 하는 신호다.
    BANK_NOT_FOUND(HttpStatus.NOT_FOUND, "BANK404_1", "존재하지 않는 은행입니다."),
    UNABLE_TO_DETECT_BANK(HttpStatus.BAD_REQUEST, "BANK400_1", "입력된 계좌번호의 은행을 추론할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
