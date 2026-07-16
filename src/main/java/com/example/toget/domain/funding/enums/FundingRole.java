package com.example.toget.domain.funding.enums;

/**
 * 펀딩 멤버(funding_members)의 역할.
 * funding_members.role 컬럼에 문자열로 저장된다.
 *
 * CREATOR(개설자) : 펀딩 생성 시 자동 부여, 변경 불가.
 * ADMIN(관리자)   : CREATOR가 지정/해제하는 위임 권한.
 * PARTICIPANT(참여자) : 기본값. 멤버로 합류하면 이 값으로 시작한다.
 *
 * TOGETHER_GIFT 전용 — MY_GIFT는 funding_members 자체를 사용하지 않는다.
 */
public enum FundingRole {
    CREATOR, ADMIN, PARTICIPANT
}