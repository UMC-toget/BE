package com.example.toget.domain.user.service;

import com.example.toget.domain.user.converter.UserAccountConverter;
import com.example.toget.domain.user.dto.UserAccountCreateResponse;
import com.example.toget.domain.user.dto.UserAccountRequest;
import com.example.toget.domain.user.dto.UserAccountResponse;
import com.example.toget.domain.user.dto.UserAccountUpdateRequest;
import com.example.toget.domain.user.entity.UserAccount;
import com.example.toget.domain.user.exception.UserErrorCode;
import com.example.toget.domain.user.exception.UserException;
import com.example.toget.domain.user.repository.UserAccountRepository;
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
        account.update(request.bankName(), request.accountOwner(), request.account()); // dirty checking으로 UPDATE
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
        UserAccount account = userAccountRepository.findById(userAccountId)
                .orElseThrow(() -> new UserException(UserErrorCode.ACCOUNT_NOT_FOUND));
        if (!account.isOwnedBy(userId)) {
            throw new UserException(UserErrorCode.ACCOUNT_FORBIDDEN);
        }
        return account;
    }
}
