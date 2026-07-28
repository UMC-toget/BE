package com.example.toget.domain.funding.exception.code;

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
    GIFT_REQUIRED_FOR_MY_GIFT(HttpStatus.BAD_REQUEST, "FUNDING400_7", "내 선물 만들기는 선물을 최소 1개 이상 등록해야 합니다."),
    VISIBILITY_REQUIRED_FOR_MY_GIFT(HttpStatus.BAD_REQUEST, "FUNDING400_8", "내 선물 만들기는 공개 여부를 설정해야 합니다."),
    NOT_ACCOUNT_OWNER(HttpStatus.BAD_REQUEST, "FUNDING400_9", "본인 소유의 계좌만 등록할 수 있습니다."),
    FUNDING_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "FUNDING400_10", "이미 종료된 펀딩은 수정 불가합니다."),
    INVALID_STATUS_FOR_PERIOD_UPDATE(HttpStatus.BAD_REQUEST, "FUNDING400_11", "SELECTING/SETTLING 외 상태에서의 기간 변경은 불가합니다."),
    END_DATE_MUST_BE_FUTURE(HttpStatus.BAD_REQUEST, "FUNDING400_12", "종료일은 오늘 이후 날짜여야 합니다."),
    NOT_MY_GIFT_TYPE(HttpStatus.BAD_REQUEST, "FUNDING400_13", "내 선물 준비하기 전용 기능입니다."),
    NOT_TOGETHER_GIFT_TYPE(HttpStatus.BAD_REQUEST, "FUNDING400_14", "함께 선물 준비하기 전용 기능입니다."),
    CONTRIBUTION_NOT_ALLOWED_FOR_STATUS(HttpStatus.BAD_REQUEST, "FUNDING400_15", "현재 펀딩 상태에서는 후원할 수 없습니다."),
    INVALID_MEMBER_ROLE(HttpStatus.BAD_REQUEST, "FUNDING400_16", "유효하지 않은 멤버 역할입니다."),
    REVIEW_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "FUNDING400_17", "후기 내용은 필수입니다."),
    REVIEW_TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "FUNDING400_18", "제목은 필수입니다."),
    REVIEW_TYPE_NOT_ALLOWED_FOR_FUNDING_TYPE(HttpStatus.BAD_REQUEST, "FUNDING400_19", "이 펀딩 유형에서는 작성할 수 없는 게시물 종류입니다."),

    FUNDING_NOT_FOUND(HttpStatus.NOT_FOUND, "FUNDING404_1", "해당 펀딩을 찾을 수 없습니다."),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "FUNDING404_2", "정산 계좌를 찾을 수 없습니다."),
    ACCOUNT_NOT_REGISTERED(HttpStatus.NOT_FOUND, "FUNDING404_3", "아직 정산 계좌가 등록되지 않았습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "FUNDING404_4", "해당 멤버를 찾을 수 없습니다."),
    CONTRIBUTION_NOT_FOUND(HttpStatus.NOT_FOUND, "FUNDING404_5", "해당 참여 기록을 찾을 수 없습니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "FUNDING404_6", "해당 게시물을 찾을 수 없습니다."),

    // TODO: 추후 수정 필요
    CHARACTER_NOT_FOUND(HttpStatus.NOT_FOUND, "FUNDING404_9", "해당 캐릭터를 찾을 수 없습니다."),
    INVITATION_BACKGROUND_NOT_FOUND(HttpStatus.NOT_FOUND, "FUNDING404_10", "해당 배경 색상을 찾을 수 없습니다."),


    NOT_FUNDING_OWNER(HttpStatus.FORBIDDEN, "FUNDING403", "펀딩 개설자만 가능한 작업입니다."),


    INVALID_FUNDING_STATUS_TRANSITION(HttpStatus.CONFLICT, "FUNDING409_1", "현재 상태에서는 해당 작업을 수행할 수 없습니다."),
    INVALID_SETTLEMENT_STATUS_TRANSITION(HttpStatus.CONFLICT, "FUNDING409_2", "현재 입금 상태에서는 해당 작업을 수행할 수 없습니다."),
    SETTLEMENT_LOCKED(HttpStatus.CONFLICT, "FUNDING409_3", "이미 입금 절차가 시작되어 정산 인원과 금액을 변경할 수 없습니다."),
    GIFT_UPDATE_NOT_ALLOWED(HttpStatus.CONFLICT, "FUNDING409_4", "최종 선물이 확정되었거나 수정 가능 기간이 아니어서 후보 선물을 변경할 수 없습니다."),
    DASHBOARD_STATUS_MISMATCH(HttpStatus.CONFLICT, "FUNDING409_5", "현재 펀딩 상태에서는 조회할 수 없습니다."),
    NOT_SETTLEMENT_TARGET_MEMBER(HttpStatus.CONFLICT, "FUNDING409_7", "정산 대상자가 아닙니다."),
    GIFT_NOT_CONFIRMED(HttpStatus.CONFLICT, "FUNDING409_8", "확정되지 않은 선물에는 구매 내역을 등록할 수 없습니다."),
    PURCHASE_ALREADY_EXISTS(HttpStatus.CONFLICT, "FUNDING409_9", "이미 구매 내역이 등록된 선물입니다."),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "FUNDING409_10", "이미 작성된 게시물입니다."),

    ;


    private final HttpStatus status;
    private final String code;
    private final String message;
}