package com.example.toget.domain.workspace.exception.code;

import com.example.toget.global.apiPayload.code.BaseSuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Workspace 도메인 성공코드 카탈로그.
 * 코드 규칙: DRAFT + HTTP상태 + _순번 (예: DRAFT200_1)
 */
@Getter
@RequiredArgsConstructor
public enum WorkspaceSuccessCode implements BaseSuccessCode {

    DRAFT_DETAIL_OK(HttpStatus.OK, "DRAFT200", "임시 저장 상세 데이터를 성공적으로 조회했습니다."),
    DRAFT_SAVE_OK(HttpStatus.OK, "DRAFT200_1", "임시 저장 작성을 성공적으로 완료했습니다."),
    DRAFT_DELETE_OK(HttpStatus.OK, "DRAFT200_2", "임시 저장 데이터를 성공적으로 삭제했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
