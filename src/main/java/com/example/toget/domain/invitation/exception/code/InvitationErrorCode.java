package com.example.toget.domain.invitation.exception.code;

import com.example.toget.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Invitation 도메인 에러코드 카탈로그.
 * 코드 규칙: 리소스명 + HTTP상태 + _순번
 */
@Getter
@RequiredArgsConstructor
public enum InvitationErrorCode implements BaseErrorCode {

    // 404 Not Found — soft delete된 리소스도 "존재하지 않음"으로 취급한다
    CHARACTER_NOT_FOUND(HttpStatus.NOT_FOUND, "CHARACTER404_1", "존재하지 않는 캐릭터입니다."),
    BACKGROUND_NOT_FOUND(HttpStatus.NOT_FOUND, "BACKGROUND404_1", "존재하지 않는 초대장 배경입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
