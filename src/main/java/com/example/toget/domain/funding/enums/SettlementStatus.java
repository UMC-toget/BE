package com.example.toget.domain.funding.enums;

/**
 * 펀딩 멤버의 정산 입금 상태.
 * funding_members.settlement_status 컬럼에 문자열로 저장된다.
 *
 * UNPAID(미입금) → PAID(확인 대기, 정산 인원 본인이 "입금 완료하기" 선택)
 *                → CONFIRMED(입금 확인, 개설자가 실제 입금 확인 후 최종 처리)
 *  - UNPAID → PAID: 정산 인원 본인만 가능
 *  - PAID → CONFIRMED: 개설자만 가능
 *
 * amountDue가 null인 멤버(=정산 대상이 아니거나 아직 정산 미확정)에게는 의미가 없으므로
 * 이 필드도 함께 null이어야 한다.
 */
public enum SettlementStatus {
    UNPAID, PAID, CONFIRMED
}