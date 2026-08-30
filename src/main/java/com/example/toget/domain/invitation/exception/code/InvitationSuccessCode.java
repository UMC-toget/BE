package com.example.toget.domain.invitation.exception.code;

import com.example.toget.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Invitation 도메인 성공코드 카탈로그.
 * 코드 규칙: 리소스명 + HTTP상태 + _순번 (에러코드 InvitationErrorCode와 대칭)
 */
@Getter
@RequiredArgsConstructor
public enum InvitationSuccessCode implements BaseSuccessCode {

    // 캐릭터(초대장 스킨)
    CHARACTER_LIST_OK(HttpStatus.OK, "CHARACTER200_1", "캐릭터 목록을 성공적으로 조회했습니다."),
    CHARACTER_UPDATE_OK(HttpStatus.OK, "CHARACTER200_2", "캐릭터를 성공적으로 수정했습니다."),
    CHARACTER_DELETE_OK(HttpStatus.OK, "CHARACTER200_3", "캐릭터를 성공적으로 삭제했습니다."),
    // 생성은 실제 HTTP 상태(@ResponseStatus 201)와 응답 본문 코드를 201로 일치시킨다
    CHARACTER_CREATE_OK(HttpStatus.CREATED, "CHARACTER201_1", "캐릭터를 성공적으로 생성했습니다."),

    // 초대장 배경 색상
    BACKGROUND_LIST_OK(HttpStatus.OK, "BACKGROUND200_1", "초대장 배경 색상 목록을 성공적으로 조회했습니다."),
    BACKGROUND_UPDATE_OK(HttpStatus.OK, "BACKGROUND200_2", "초대장 배경 색상을 성공적으로 수정했습니다."),
    BACKGROUND_DELETE_OK(HttpStatus.OK, "BACKGROUND200_3", "초대장 배경 색상을 성공적으로 삭제했습니다."),
    BACKGROUND_CREATE_OK(HttpStatus.CREATED, "BACKGROUND201_1", "초대장 배경 색상을 성공적으로 생성했습니다."),

    // 초대장 카드
    INVITATION_UPDATE_OK(HttpStatus.OK, "INVITATION200_1", "초대장을 성공적으로 수정했습니다."),
    INVITATION_GET_OK(HttpStatus.OK, "INVITATION200_2", "초대장을 성공적으로 조회했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
