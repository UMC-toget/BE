package com.example.toget.domain.workspace.exception.code;

import com.example.toget.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Workspace 도메인 에러코드 카탈로그.
 * 코드 규칙: DRAFT + HTTP상태 (예: DRAFT404)
 */
@Getter
@RequiredArgsConstructor
public enum WorkspaceErrorCode implements BaseErrorCode {

    DRAFT_NOT_FOUND(HttpStatus.NOT_FOUND, "DRAFT404_1", "임시 저장 데이터가 존재하지 않습니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "DRAFT400_2", "종료일은 시작일보다 빠를 수 없습니다."),
    INVALID_END_DATE(HttpStatus.BAD_REQUEST, "DRAFT400_3", "종료일은 선물 전달 날짜(기념일)보다 늦을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
