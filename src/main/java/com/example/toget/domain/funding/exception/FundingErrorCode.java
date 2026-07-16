package com.example.toget.domain.funding.exception;

import com.example.toget.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FundingErrorCode implements BaseErrorCode {

    INVALID_FUNDING_PERIOD(HttpStatus.BAD_REQUEST, "FUNDING400_1", "시작일은 종료일보다 앞서야 합니다."),
    ACCOUNT_REQUIRED_FOR_MY_GIFT(HttpStatus.BAD_REQUEST, "FUNDING400_2", "내 선물 페이지는 정산 계좌 등록이 필수입니다."),
    CREATOR_ROLE_CANNOT_BE_CHANGED(HttpStatus.BAD_REQUEST, "FUNDING400_3", "개설자의 역할은 변경할 수 없습니다."),
    NOT_SETTLEMENT_TARGET(HttpStatus.BAD_REQUEST, "FUNDING400_4", "정산 대상이 아닌 멤버입니다."),
    VOTE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "FUNDING400_5", "투표는 최대 3개까지 가능합니다."),
    INVALID_TARGET_AMOUNT(HttpStatus.BAD_REQUEST, "FUNDING400_6", "목표 금액은 0원 이상 필수로 입력해야 합니다."),


    INVALID_FUNDING_STATUS_TRANSITION(HttpStatus.CONFLICT, "FUNDING409_1", "현재 상태에서는 해당 작업을 수행할 수 없습니다."),
    INVALID_SETTLEMENT_STATUS_TRANSITION(HttpStatus.CONFLICT, "FUNDING409_2", "현재 입금 상태에서는 해당 작업을 수행할 수 없습니다."),
    SETTLEMENT_LOCKED(HttpStatus.CONFLICT, "FUNDING409_3", "이미 입금 절차가 시작되어 정산 인원과 금액을 변경할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}