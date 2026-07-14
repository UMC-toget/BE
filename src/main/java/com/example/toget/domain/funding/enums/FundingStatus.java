package com.example.toget.domain.funding.enums;

/**
 * 펀딩(선물 준비) 진행 상태.
 * fundings.status 컬럼에 문자열로 저장된다.
 *
 * SELECTING(선물 후보 선정 중) → SETTLING(정산 중) → ENDED(종료)
 * 순서로만 전이되며 역방향 전이는 없다.
 *
 * MY_GIFT 유형은 후보 선정 단계 자체가 없으므로 생성 시점에 바로 SETTLING으로 시작한다.
 * TOGETHER_GIFT 유형만 SELECTING에서 시작해, 개최자가 정산 인원/금액까지 확정하는 시점(4단계 "정산 시작하기")에 SETTLING으로 전이한다.
 *
 * ENDED는 "준비 완료(최종 선물 확정됨)"와 "종료(최종 선물 미확정)" 두 케이스를 모두
 * 포함한다. 이 둘을 구분해야 할 때는 FundingGift에 status=SELECTED인 row가
 * 존재하는지로 판단한다
 */
public enum FundingStatus {
    SELECTING,  // 선물 후보 선정 중
    SETTLING,   // 정산 중 (TOGETHER_GIFT: 정산 확정됨)
    ENDED,  // 종료
    }
