package com.example.toget.domain.user.enums;

import com.example.toget.domain.user.exception.UserErrorCode;
import com.example.toget.domain.user.exception.UserException;

/** 지원하는 소셜 로그인 공급자 */
public enum OAuthProvider {
    KAKAO, GOOGLE;

    /**
     * URL 경로의 문자열("kakao")을 enum으로 변환.
     * valueOf는 대문자만 인식하고 실패 시 IllegalArgumentException을 던지므로,
     * 대소문자를 정규화하고 미지원 값은 우리 표준 예외(400)로 감싸서 던진다.
     */
    public static OAuthProvider from(String value) {
        try {
            return OAuthProvider.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new UserException(UserErrorCode.UNSUPPORTED_PROVIDER);
        }
    }
}
