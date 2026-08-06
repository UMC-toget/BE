package com.example.toget.domain.bank.repository;

import com.example.toget.domain.bank.entity.Bank;
import com.example.toget.global.enums.BankName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** banks 테이블 접근 리포지토리 */
public interface BankRepository extends JpaRepository<Bank, Long> {

    // findAllBy + Active + True + OrderBy + SortOrder + Asc
    // → SELECT * FROM banks WHERE is_active = true ORDER BY sort_order ASC
    // (은행 선택 바텀시트에 노출할 목록 — 비활성 은행은 신규 선택지에서 제외)
    // 프로퍼티명이 active인 이유는 Bank.active 필드 주석 참고
    List<Bank> findAllByActiveTrueOrderBySortOrderAsc();

    // 계좌 등록/수정 시 요청의 BankName으로 은행 행을 찾을 때 사용
    Optional<Bank> findByCode(BankName code);
}
