package com.example.toget.domain.funding.exception.code;

import com.example.toget.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FundingSuccessCode implements BaseSuccessCode {

    // 펀딩 페이지 상태 관련
    FUNDING_CREATE_OK(HttpStatus.OK, "FUNDING200_1", "선물 준비가 성공적으로 개설되었습니다."),
    FUNDING_GIFTS_UPDATE_OK(HttpStatus.OK, "FUNDING200_2", "후보 선물 목록이 성공적으로 갱신되었습니다."),
    FUNDING_STATUS_UPDATE_OK(HttpStatus.OK, "FUNDING200_3", "선물 준비가 성공적으로 종료되었습니다."),

    // 펀딩 페이지 기본 정보 관련
    FUNDING_DASHBOARD_OK(HttpStatus.OK, "FUNDING200_4", "선물 준비 상세 정보를 성공적으로 조회했습니다."),
    FUNDING_BASIC_INFO_UPDATE_OK(HttpStatus.OK, "FUNDING200_5", "기본정보가 성공적으로 수정되었습니다."),
    FUNDING_ACCOUNT_GET_OK(HttpStatus.OK, "FUNDING200_6", "정산 계좌 정보를 성공적으로 조회했습니다."),
    FUNDING_ACCOUNT_UPDATE_OK(HttpStatus.OK, "FUNDING200_7", "정산 계좌가 성공적으로 변경되었습니다."),
    FUNDING_MEMBERS_GET_OK(HttpStatus.OK, "FUNDING200_8", "참여자 관리 정보를 성공적으로 조회했습니다."),
    FUNDING_MEMBER_ROLE_UPDATE_OK(HttpStatus.OK, "FUNDING200_9", "멤버 역할이 성공적으로 변경되었습니다."),
    FUNDING_SETTLEMENTS_GET_OK(HttpStatus.OK, "FUNDING200_10", "정산 내역을 성공적으로 조회했습니다."),
    FUNDING_SETTLEMENT_STATUS_UPDATE_OK(HttpStatus.OK, "FUNDING200_11", "정산 입금 상태가 성공적으로 변경되었습니다."),
    FUNDING_CONTRIBUTIONS_GET_OK(HttpStatus.OK, "FUNDING200_12", "참여자 목록을 성공적으로 조회했습니다."),
    FUNDING_CONTRIBUTION_AMOUNT_UPDATE_OK(HttpStatus.OK, "FUNDING200_13", "후원 금액이 성공적으로 수정되었습니다."),

    CONTRIBUTION_CREATE_OK(HttpStatus.OK, "FUNDING200_15", "참여가 성공적으로 등록되었습니다."),
    CONTRIBUTION_LIST_OK(HttpStatus.OK, "FUNDING200_16", "축하 메시지 목록을 성공적으로 조회했습니다."),
    CONTRIBUTION_DETAIL_OK(HttpStatus.OK, "FUNDING200_17", "축하 메시지를 성공적으로 조회했습니다."),

    MY_GIFT_DASHBOARD_OK(HttpStatus.OK, "FUNDING200_18", "내 선물 페이지 대시보드를 성공적으로 조회했습니다."),
    TOGETHER_GIFT_DASHBOARD_OK(HttpStatus.OK, "FUNDING200_19", "함께 선물하기 대시보드를 성공적으로 조회했습니다."),
    CONFIRM_SETTLEMENT_OK(HttpStatus.OK, "FUNDING200_20", "선물과 정산 참여자가 성공적으로 확정되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}