package com.example.toget.domain.user.entity;

import com.example.toget.domain.user.enums.BankName;
import com.example.toget.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 정산 계좌 엔티티 — user_accounts 테이블 매핑. 한 회원이 계좌를 여러 개 등록할 수 있다(1:N).
 *
 * [연관관계 없이 Long userId를 쓰는 이유]
 * @ManyToOne User 대신 FK 값만 저장하면 계좌 조회 시 User까지 딸려 오는 문제(N+1 등)가 없고
 * 도메인 간 결합도 낮아진다. User 정보가 필요하면 그때 UserRepository로 명시적으로 조회한다.
 */
@Entity
@Table(name = "user_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA용 기본 생성자, 외부 사용은 차단
public class UserAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_account_id")
    private Long id;

    /** 소유자 FK — 소유권 검사(isOwnedBy)의 기준 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING) // 은행명을 "KAKAO_BANK" 같은 이름 문자열로 저장
    @Column(name = "bank_name", nullable = false, length = 30)
    private BankName bankName;

    /** 예금주명 */
    @Column(name = "account_owner", nullable = false, length = 50)
    private String accountOwner;

    /** 계좌번호 */
    @Column(name = "account", nullable = false, length = 50)
    private String account;

    @Builder
    private UserAccount(Long userId, BankName bankName, String accountOwner, String account) {
        this.userId = userId;
        this.bankName = bankName;
        this.accountOwner = accountOwner;
        this.account = account;
    }

    /** null인 필드는 건드리지 않는 부분 수정(PATCH) — 트랜잭션 커밋 시 dirty checking으로 UPDATE된다 */
    public void update(BankName bankName, String accountOwner, String account) {
        if (bankName != null) {
            this.bankName = bankName;
        }
        if (accountOwner != null) {
            this.accountOwner = accountOwner;
        }
        if (account != null) {
            this.account = account;
        }
    }

    /** 소유권 검사 — 서비스 계층에서 남의 계좌 접근(IDOR)을 막을 때 사용 */
    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }
}
