package com.example.toget.domain.user.dto;

/**
 * 내 프로필 조회 응답.
 * User 엔티티에서 공개 가능한 필드만 골라 담는다 — refreshToken, status 같은 내부 상태는 제외.
 */
public record UserProfileResponse(
        Long userId,
        String email,
        String nickname,
        String name,
        String profileImageUrl,
        String oauthProvider // "KAKAO" / "GOOGLE" — 어느 소셜 계정으로 가입했는지 표시용
) {
}
