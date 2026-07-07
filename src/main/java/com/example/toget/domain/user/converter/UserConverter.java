package com.example.toget.domain.user.converter;

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
                user.getOauthProvider().name() // enum → "KAKAO" 같은 문자열로
        );
    }

    public static UserProfileUpdateResponse toProfileUpdateResponse(User user) {
        return new UserProfileUpdateResponse(user.getId(), user.getNickname(), user.getProfileImageUrl());
    }

    public static SocialLoginResponse toSocialLoginResponse(User user, String accessToken, String refreshToken,
                                                            boolean isNewUser) {
        return new SocialLoginResponse(user.getId(), user.getEmail(), user.getName(),
                accessToken, refreshToken, isNewUser);
    }
}
