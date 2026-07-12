package com.example.toget.domain.funding.enums;

/**
 * 펀딩 멤버의 정산 입금 상태.
 * funding_members.settlement_status 컬럼에 문자열로 저장된다.
 *
 * UNPAID(미입금) → PAID(입금완료, 참여자 자기신고) → CONFIRMED(확인완료, 개설자가 계좌 대조 후 확정)
 * 정해진 순서가 있지만, 개설자는 이 상태를 언제든 임의로 변경할 수 있다
 * (예: CONFIRMED로 잘못 확인한 걸 되돌리는 경우).
 *
 * amountDue가 null인 멤버(=정산 대상이 아니거나 아직 정산 미확정)에게는 의미가 없으므로
 * 이 필드도 함께 null이어야 한다.
 */
public enum SettlementStatus {
    UNPAID, PAID, CONFIRMED
}