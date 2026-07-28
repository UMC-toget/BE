package com.example.toget.domain.user.converter;

import com.example.toget.domain.user.dto.OAuthUserInfo;
import com.example.toget.domain.user.dto.SignupCompleteResponse;
import com.example.toget.domain.user.dto.SocialLoginResponse;
import com.example.toget.domain.user.dto.UserProfileResponse;
import com.example.toget.domain.user.dto.UserProfileUpdateResponse;
import com.example.toget.domain.user.entity.User;

/**
 * User 엔티티 → 응답 DTO 변환 전담 클래스.
 * 엔티티를 API 응답으로 직접 내보내면 refreshToken 같은 내부 필드가 노출되고,
 * DB 스키마 변경이 곧바로 API 스펙 변경이 되어 버린다. 그래서 변환 계층을 한 곳에 둔다.
 * 상태가 없으므로 static 메서드로만 구성 (private 생성자로 인스턴스화 차단).
 */
public class UserConverter {

    private UserConverter() {
    }

    public static UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getName(),
                user.getProfileImageUrl(),
                user.getOAuthProvider().name() // enum → "KAKAO" 같은 문자열로
        );
    }

    public static UserProfileUpdateResponse toProfileUpdateResponse(User user) {
        return new UserProfileUpdateResponse(user.getId(), user.getNickname(), user.getProfileImageUrl());
    }

    /** 기존 회원 로그인 — 토큰을 그대로 실어 보낸다 */
    public static SocialLoginResponse toSocialLoginResponse(User user, String accessToken, String refreshToken) {
        return new SocialLoginResponse(user.getId(), user.getEmail(), user.getName(),
                accessToken, refreshToken, null, true);
    }

    /**
     * 미가입자 로그인 — 아직 User 엔티티가 없으므로 소셜에서 받아온 정보로만 응답을 만든다.
     * userId·accessToken·refreshToken이 모두 null인 것이 정상이며,
     * 프론트는 isProfileCompleted=false를 보고 프로필 설정 화면으로 이동해야 한다. (issue #61)
     */
    public static SocialLoginResponse toSignupRequiredResponse(OAuthUserInfo info, String signupToken) {
        return new SocialLoginResponse(null, info.email(), info.name(),
                null, null, signupToken, false);
    }

    public static SignupCompleteResponse toSignupCompleteResponse(User user, String accessToken, String refreshToken) {
        return new SignupCompleteResponse(user.getId(), user.getEmail(), user.getName(),
                user.getNickname(), user.getProfileImageUrl(), accessToken, refreshToken);
    }
}
