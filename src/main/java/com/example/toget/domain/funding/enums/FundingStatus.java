package com.example.toget.domain.funding.enums;

/**
 * 펀딩(선물 준비) 진행 상태.
 * fundings.status 컬럼에 문자열로 저장된다.
 *
 * GIFT_SELECTING(선물 후보 선정 중) → ACTIVE(펀딩 진행 중) → ENDED(선물 전달 완료)
 * 순서로만 전이되며 역방향 전이는 없다.
 *
 * MY_GIFT 유형은 후보 선정 단계 자체가 없으므로 생성 시점에 바로 ACTIVE로 시작한다.
 * TOGETHER_GIFT 유형만 GIFT_SELECTING에서 시작해, 개최자가 최종 선물을 확정하는 시점에 ACTIVE로 전이한다.
 */
public enum FundingStatus {
    GIFT_SELECTING, // 선물 후보 선정 중
    ACTIVE, // 펀딩 진행 중
    ENDED // 선물 전달 완료
}
