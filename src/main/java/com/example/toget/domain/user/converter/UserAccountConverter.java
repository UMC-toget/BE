package com.example.toget.domain.user.converter;

import com.example.toget.domain.user.dto.UserAccountCreateResponse;
import com.example.toget.domain.user.dto.UserAccountResponse;
import com.example.toget.domain.user.entity.UserAccount;

/** UserAccount 엔티티 → 계좌 응답 DTO 변환 (UserConverter와 같은 역할의 변환 전담 클래스) */
public class UserAccountConverter {

    private UserAccountConverter() {
    }

    public static UserAccountResponse toAccountResponse(UserAccount account) {
        // 아이콘·표시명은 엔티티의 널가드 메서드를 거친다 — 백필 전 레거시 행은 bank가 null이다
        return new UserAccountResponse(account.getId(), account.getBankName(),
                account.getBankDisplayName(), account.getBankIconUrl(),
                account.getAccountOwner(), account.getAccount());
    }

    /** 생성 응답은 새로 발급된 ID만 내려준다 — 나머지는 클라이언트가 이미 아는 값이므로 */
    public static UserAccountCreateResponse toCreateResponse(UserAccount account) {
        return new UserAccountCreateResponse(account.getId());
    }
}
