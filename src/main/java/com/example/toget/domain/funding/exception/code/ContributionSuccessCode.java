package com.example.toget.domain.funding.exception.code;

import com.example.toget.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ContributionSuccessCode implements BaseSuccessCode {

    BACKGROUND_LIST_OK(HttpStatus.OK, "METADATA200_1", "배경 색상 목록을 성공적으로 조회했습니다."),
    BACKGROUND_CREATE_OK(HttpStatus.OK, "METADATA200_2", "배경 색상을 성공적으로 생성했습니다."),
    BACKGROUND_UPDATE_OK(HttpStatus.OK, "METADATA200_3", "배경 색상을 성공적으로 수정했습니다."),
    BACKGROUND_DELETE_OK(HttpStatus.OK, "METADATA200_4", "배경 색상을 성공적으로 삭제했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}