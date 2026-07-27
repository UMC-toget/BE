package com.example.toget.domain.gift.exception.code;

import com.example.toget.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FundingGiftErrorCode implements BaseErrorCode {
    DUPLICATE_GIFT_ID_IN_REQUEST(HttpStatus.BAD_REQUEST, "GIFT400_3", "요청에 동일한 선물 ID가 중복으로 포함되어 있습니다."),
    NOT_FUNDING_MEMBER(HttpStatus.FORBIDDEN, "GIFT403_1", "해당 펀딩의 멤버가 아닙니다."),
    NOT_CREATOR_OR_ADMIN(HttpStatus.FORBIDDEN, "GIFT403_2", "개설자 또는 관리자만 가능한 작업입니다."),

    FUNDING_GIFT_NOT_FOUND(HttpStatus.NOT_FOUND, "GIFT404_1", "펀딩 선물을 찾을 수 없습니다."),

    VOTE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "GIFT400_2", "투표는 최대 3개까지 가능합니다."),
    GIFT_ALREADY_SELECTED(HttpStatus.CONFLICT, "GIFT409_2", "이미 확정된 선물에는 투표할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}