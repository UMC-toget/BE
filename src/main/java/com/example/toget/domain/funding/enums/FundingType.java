package com.example.toget.domain.funding.enums;

/**
 * 펀딩(선물 준비) 유형.
 * fundings.funding_type 컬럼에 문자열로 저장된다.
 *
 * MY_GIFT       : 내 선물 만들기 — 개최자 본인이 받고 싶은 선물을 위해 여는 펀딩.
 *                 PG 연동 없이 계좌이체 방식이며, 참여(Contribution)는 입금 확인 절차 없이
 *                 즉시 COMPLETED로 기록된다 (Contribution.createForMyGift 참고).
 * TOGETHER_GIFT : 함께 선물하기 — 여러 참여자가 제3자에게 줄 선물을 위해 모으는 펀딩.
 *                 개최자가 참여자별 입금 여부를 수동으로 확인해야 하므로 참여는
 *                 PENDING으로 시작한다 (Contribution.createForTogetherGift 참고).
 */
public enum FundingType {
    MY_GIFT, TOGETHER_GIFT
}