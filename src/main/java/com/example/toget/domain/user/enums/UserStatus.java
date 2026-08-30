package com.example.toget.domain.user.enums;

/**
 * 회원 상태 — users.status 컬럼에 문자열로 저장된다.
 * ACTIVE(정상) / SUSPENDED(정지) / PENDING(대기) / WITHDRAWN(탈퇴 — Soft Delete 표식)
 * 현재 로직은 ACTIVE만 "활성"으로 취급한다 (User.isActive 참고).
 */
public enum UserStatus {
    ACTIVE, SUSPENDED, PENDING, WITHDRAWN
}
