package com.example.toget.domain.user.service;

/**
 * JWT 검증(parse) 결과로 꺼낸 클레임 묶음.
 * record: 필드·생성자·getter(userId() 형태)·equals/hashCode를 자동 생성하는 불변 데이터 클래스.
 */
public record JwtClaims(
        Long userId, // sub 클레임 — 토큰 주인의 사용자 ID
        String id,   // jti 클레임 — refresh token 재사용 감지용 고유 ID (access token은 null)
        String type  // typ 클레임 — "access" / "refresh"
) {
}
