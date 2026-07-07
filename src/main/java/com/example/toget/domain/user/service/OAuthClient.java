package com.example.toget.domain.user.service;

import com.example.toget.domain.user.dto.OAuthUserInfo;
import com.example.toget.domain.user.enums.OauthProvider;

/**
 * 소셜 로그인 공급자별 토큰 검증을 추상화한 인터페이스 (전략 패턴).
 * AuthService는 이 인터페이스만 알고, 카카오/구글별 세부 구현은 각 구현체가 감춘다.
 * 새 공급자(네이버 등) 추가 시 구현체 하나만 만들면 나머지 코드는 그대로 동작한다.
 */
public interface OAuthClient {

    /** 이 구현체가 담당하는 공급자 — AuthService가 요청의 provider와 매칭할 때 사용 */
    OauthProvider provider();

    /**
     * 공급자에게 identityToken을 검증하고 사용자 정보를 가져온다.
     * 검증 실패(만료·위조 등) 시 USER401 UserException을 던진다.
     */
    OAuthUserInfo verify(String identityToken);
}
