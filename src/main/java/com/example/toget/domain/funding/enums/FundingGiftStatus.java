package com.example.toget.domain.funding.enums;


/**
 * 후보 선물의 상태.
 * funding_gifts.status 컬럼에 문자열로 저장된다.
 *
 * CANDIDATE(후보) : 등록 시 기본값.
 * SELECTED(최종 선택) : 개최자가 "선물 확정하기" 단계에서 확정한 선물.
 *               한 펀딩에 SELECTED가 여러 개 존재할 수 있다 (선물 여러 개 확정 가능).
 */
public enum FundingGiftStatus {
    CANDIDATE, SELECTED
}
