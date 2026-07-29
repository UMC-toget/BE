package com.example.toget.domain.gift.exception.code;

import com.example.toget.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FundingGiftSuccessCode implements BaseSuccessCode {

    GIFT_CANDIDATE_LIST_OK(HttpStatus.OK, "GIFT200_1", "선물 후보 목록을 성공적으로 조회했습니다."),
    GIFT_CANDIDATE_CREATE_OK(HttpStatus.OK, "GIFT200_2", "선물 후보가 성공적으로 등록되었습니다."),
    GIFT_CANDIDATE_DETAIL_OK(HttpStatus.OK, "GIFT200_3", "선물 후보 상세 정보를 성공적으로 조회했습니다."),
    GIFT_VOTE_TOGGLE_OK(HttpStatus.OK, "GIFT200_4", "투표가 성공적으로 처리되었습니다."),
    GIFT_COMMENT_CREATE_OK(HttpStatus.OK, "GIFT200_5", "댓글이 성공적으로 등록되었습니다."),
    GIFT_PURCHASE_UPLOAD_OK(HttpStatus.OK, "GIFT200_6", "구매 내역이 성공적으로 등록되었습니다."),
    GIFT_LIST_UPDATE_OK(HttpStatus.OK, "GIFT200_7", "선물 목록이 성공적으로 수정되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}