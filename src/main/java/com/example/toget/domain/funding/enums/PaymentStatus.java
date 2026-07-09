package com.example.toget.domain.funding.enums;

/**
 * 펀딩 참여(정산)의 입금 상태.
 * PENDING(입금대기) → COMPLETED(입금완료) 또는 CANCELLED(환불/취소)로 전이된다.
 * COMPLETED/CANCELLED는 종결 상태이며, 이후 다른 상태로 되돌아가지 않는다.
 */
public enum PaymentStatus {
    PENDING, COMPLETED, CANCELLED
}
