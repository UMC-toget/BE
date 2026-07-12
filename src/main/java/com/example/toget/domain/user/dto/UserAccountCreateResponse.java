package com.example.toget.domain.user.dto;

/** 계좌 생성 응답 — 새로 발급된 ID만 반환 (나머지는 클라이언트가 보낸 값 그대로이므로) */
public record UserAccountCreateResponse(
        Long userAccountId
) {
}
