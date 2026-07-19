package com.example.toget.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 지원 은행 목록.
 * 자유 문자열 대신 enum으로 받아서 오타·비표준 은행명이 DB에 들어가는 것을 원천 차단한다
 * (목록에 없는 값은 요청 역직렬화 단계에서 거부됨).
 */
@Getter
@RequiredArgsConstructor
public enum BankName {
    KB("KB국민은행"),
    SHINHAN("신한은행"),
    WOORI("우리은행"),
    HANA("하나은행"),
    NH("NH농협은행"),
    IBK("IBK기업은행"),
    SC("SC제일은행"),
    CITI("한국씨티은행"),
    KAKAO_BANK("카카오뱅크"),
    TOSS_BANK("토스뱅크"),
    K_BANK("케이뱅크"),
    POST_OFFICE("우체국"),
    MG_SAEMAEUL("새마을금고"),
    SHINHYUP("신협"),
    SUHYUP("수협은행"),
    BUSAN("부산은행"),
    IM_BANK("iM뱅크"),
    GWANGJU("광주은행"),
    JEONBUK("전북은행"),
    GYEONGNAM("경남은행"),
    JEJU("제주은행"),
    KDB("KDB산업은행");

    /** 한국어 표시명 */
    private final String displayName;
}
