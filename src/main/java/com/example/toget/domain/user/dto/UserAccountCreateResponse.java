package com.example.toget.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 계좌 생성 응답 — 새로 발급된 ID만 반환 (나머지는 클라이언트가 보낸 값 그대로이므로) */
public record UserAccountCreateResponse(
        @Schema(description = "생성된 사용자 계좌 ID", example = "1")
        Long userAccountId
) {
}
