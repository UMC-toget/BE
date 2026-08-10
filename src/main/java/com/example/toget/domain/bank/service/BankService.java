package com.example.toget.domain.bank.service;

import com.example.toget.domain.bank.converter.BankConverter;
import com.example.toget.domain.bank.dto.BankResponse;
import com.example.toget.domain.bank.dto.BankUpdateRequest;
import com.example.toget.domain.bank.entity.Bank;
import com.example.toget.domain.bank.exception.BankException;
import com.example.toget.domain.bank.exception.code.BankErrorCode;
import com.example.toget.domain.bank.repository.BankRepository;
import com.example.toget.domain.bank.dto.BankDetectionRequest;
import com.example.toget.domain.bank.dto.BankDetectionResponse;
import com.example.toget.domain.bank.util.BankDetector;
import com.example.toget.global.enums.BankName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 은행 마스터 데이터 조회/수정 비즈니스 로직.
 *
 * <p>은행은 특정 사용자의 소유물이 아니라 서비스 전체가 공유하는 마스터 데이터이므로,
 * 캐릭터·초대장 배경과 마찬가지로 조회는 전체 공개, 변경은 관리자만 가능하다.
 * 소유권 검사(isOwnedBy)가 없는 것도 같은 이유다.
 */
@Service
@RequiredArgsConstructor
public class BankService {

    private final BankRepository bankRepository;

    /** 활성 은행 전체 조회 — 은행 선택 바텀시트의 선택지 목록. 노출 순서(sortOrder) 오름차순 */
    @Transactional(readOnly = true)
    public List<BankResponse> getActiveBanks() {
        return bankRepository.findAllByActiveTrueOrderBySortOrderAsc().stream()
                .map(BankConverter::toResponse)
                .toList();
    }

    /** 계좌번호 기반 은행 추론 */
    @Transactional(readOnly = true)
    public BankDetectionResponse detectBank(BankDetectionRequest request) {
        BankName detectedBankName = BankDetector.detect(request.accountNumber())
                .orElseThrow(() -> new BankException(BankErrorCode.UNABLE_TO_DETECT_BANK));

        Bank bank = bankRepository.findByCode(detectedBankName)
                .orElse(null);

        return BankConverter.toDetectionResponse(detectedBankName, bank);
    }

    /** 부분 수정(PATCH) — 요청에 없는(null) 필드는 기존 값 유지. 관리자 전용 */
    @Transactional
    public BankResponse update(Long bankId, BankUpdateRequest request) {
        Bank bank = bankRepository.findById(bankId)
                .orElseThrow(() -> new BankException(BankErrorCode.BANK_NOT_FOUND));
        bank.update(request.displayName(), request.iconUrl(), request.sortOrder(), request.isActive());
        return BankConverter.toResponse(bank); // dirty checking으로 UPDATE
    }
}
