package com.example.toget.domain.funding.exception.code;

import com.example.toget.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FundingSuccessCode implements BaseSuccessCode {

    FUNDING_CREATE_OK(HttpStatus.OK, "FUNDING200_1", "선물 준비가 성공적으로 개설되었습니다."),
    FUNDING_GIFTS_UPDATE_OK(HttpStatus.OK, "FUNDING200_2", "후보 선물 목록이 성공적으로 갱신되었습니다."),
    FUNDING_STATUS_UPDATE_OK(HttpStatus.OK, "FUNDING200_3", "선물 준비가 성공적으로 종료되었습니다."),
    FUNDING_DASHBOARD_OK(HttpStatus.OK, "FUNDING200_4", "선물 준비 상세 정보를 성공적으로 조회했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}