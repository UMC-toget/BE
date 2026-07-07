package com.example.toget.domain.user.service;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * 서명 비교 전용 유틸.
 * final + private 생성자: 상속·인스턴스 생성을 막은 정적 메서드 전용 클래스 관례.
 */
final class MessageDigestUtil {

    private MessageDigestUtil() {
    }

    /**
     * 상수 시간(constant-time) 문자열 비교.
     * String.equals는 다른 문자를 만나면 즉시 false를 반환해 "몇 글자까지 맞췄는지"가
     * 응답 시간으로 새어 나갈 수 있다(타이밍 공격). MessageDigest.isEqual은
     * 길이가 같으면 항상 전체를 비교하므로 서명·해시 비교에는 이쪽을 쓴다.
     */
    static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
