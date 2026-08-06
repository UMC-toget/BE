package com.example.toget.domain.user.service;

import com.example.toget.domain.bank.entity.Bank;
import com.example.toget.domain.bank.exception.BankException;
import com.example.toget.domain.bank.exception.code.BankErrorCode;
import com.example.toget.domain.bank.repository.BankRepository;
import com.example.toget.domain.user.converter.UserAccountConverter;
import com.example.toget.domain.user.dto.UserAccountCreateResponse;
import com.example.toget.domain.user.dto.UserAccountRequest;
import com.example.toget.domain.user.dto.UserAccountResponse;
import com.example.toget.domain.user.dto.UserAccountUpdateRequest;
import com.example.toget.domain.user.entity.UserAccount;
import com.example.toget.domain.user.exception.UserErrorCode;
import com.example.toget.domain.user.exception.UserException;
import com.example.toget.domain.user.repository.UserAccountRepository;
import com.example.toget.global.enums.BankName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 정산 계좌 CRUD 비즈니스 로직.
 *
 * 모든 메서드가 두 가지 보안 규칙을 지킨다:
 *  1. 탈퇴한 회원 차단 — 토큰이 아직 유효해도 활성 사용자만 허용 (activeUserReader)
 *  2. 소유권 검사 — URL의 userAccountId가 남의 계좌면 403 (getOwnedAccount)
 *     이 검사가 없으면 ID만 바꿔서 남의 자원에 접근하는 IDOR 취약점이 된다.
 */
@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final BankRepository bankRepository;
    private final ActiveUserReader activeUserReader;

    /** 등록 계좌 전체 조회 — 없으면 빈 배열 */
    @Transactional(readOnly = true)
    public List<UserAccountResponse> getMyAccounts(Long userId) {
        activeUserReader.getActiveUser(userId);
        return userAccountRepository.findAllByUserIdOrderByIdAsc(userId).stream()
                .map(UserAccountConverter::toAccountResponse) // 메서드 참조 — account -> toAccountResponse(account)와 동일
                .toList();
    }

    @Transactional
    public UserAccountCreateResponse create(Long userId, UserAccountRequest request) {
        activeUserReader.getActiveUser(userId);
        // userId는 요청 본문이 아니라 토큰에서 나온 값을 사용 — 본문으로 받으면 남의 명의로 등록 가능
        UserAccount account = userAccountRepository.save(UserAccount.builder()
                .userId(userId)
                .bankName(request.bankName())
                .bank(getBank(request.bankName()))
                .accountOwner(request.accountOwner())
                .account(request.account())
                .build());
        return UserAccountConverter.toCreateResponse(account);
    }

    /** 부분 수정(PATCH) — 요청에 없는(null) 필드는 기존 값 유지 */
    @Transactional
    public UserAccountResponse update(Long userId, Long userAccountId, UserAccountUpdateRequest request) {
        activeUserReader.getActiveUser(userId);
        UserAccount account = getOwnedAccount(userId, userAccountId);
        // 은행을 안 바꾸는 요청(bankName == null)이면 은행 조회 자체를 건너뛴다 — PATCH 의미론 유지
        Bank bank = request.bankName() == null ? null : getBank(request.bankName());
        account.update(request.bankName(), bank, request.accountOwner(), request.account()); // dirty checking으로 UPDATE
        return UserAccountConverter.toAccountResponse(account);
    }

    @Transactional
    public void delete(Long userId, Long userAccountId) {
        activeUserReader.getActiveUser(userId);
        UserAccount account = getOwnedAccount(userId, userAccountId);
        userAccountRepository.delete(account);
    }

    /** 계좌 조회 + 소유권 검사: 없으면 404, 남의 계좌면 403 */
    private UserAccount getOwnedAccount(Long userId, Long userAccountId) {
        // 수정 응답에도 은행 아이콘이 나가므로 bank를 함께 읽는 조회를 쓴다
        UserAccount account = userAccountRepository.findWithBankById(userAccountId)
                .orElseThrow(() -> new UserException(UserErrorCode.ACCOUNT_NOT_FOUND));
        if (!account.isOwnedBy(userId)) {
            throw new UserException(UserErrorCode.ACCOUNT_FORBIDDEN);
        }
        return account;
    }

    /**
     * 은행 코드로 은행 마스터 데이터를 조회한다.
     *
     * <p>요청의 bankName은 enum이라 값 자체는 이미 검증된 상태다. 그런데도 여기서 404가 날 수 있는 경우는
     * "enum에는 있는데 banks 테이블에는 없는" 상황, 즉 BankSeeder가 돌지 않은 비정상 상태뿐이다.
     * 조용히 bank=null로 저장하면 아이콘만 안 나오는 형태로 문제가 묻히므로, 명시적으로 실패시킨다.
     */
    private Bank getBank(BankName bankName) {
        return bankRepository.findByCode(bankName)
                .orElseThrow(() -> new BankException(BankErrorCode.BANK_NOT_FOUND));
    }
}
