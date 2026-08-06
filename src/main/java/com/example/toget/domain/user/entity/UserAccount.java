package com.example.toget.domain.user.entity;

import com.example.toget.domain.bank.entity.Bank;
import com.example.toget.global.enums.BankName;
import com.example.toget.global.entity.BaseEntity;
import com.example.toget.global.util.AesGcmConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 정산 계좌 엔티티 — user_accounts 테이블 매핑. 한 회원이 계좌를 여러 개 등록할 수 있다(1:N).
 *
 * [연관관계 없이 Long userId를 쓰는 이유]
 * ManyToOne User 대신 FK 값만 저장하면 계좌 조회 시 User까지 딸려 오는 문제(N+1 등)가 없고
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

    /**
     * 은행 마스터 데이터 FK — 아이콘 URL·표시명을 얻기 위한 연관관계.
     *
     * <p>[bankName과 중복 아닌가]
     * 과도기 동안 의도적으로 병행 유지한다. bank_id는 신규 컬럼이라 기존 행이 전부 null이고,
     * 백필 SQL을 돌리기 전까지는 bank_name이 유일한 은행 정보다.
     * 백필 완료 후 안정화되면 bank_name 컬럼을 제거한다.
     *
     * <p>[nullable인 이유]
     * ddl-auto=update 환경에서 nullable=false로 만들면 기존 행 때문에 스키마 갱신 자체가 실패한다.
     * 백필 전 레거시 행은 bank가 null이므로, 응답 변환 시 널가드가 필요하다.
     *
     * <p>[LAZY인 이유]
     * 계좌 목록을 조회할 때마다 은행을 즉시 로딩하면 계좌 N개당 쿼리 N번(N+1)이 나간다.
     * 대신 실제로 은행 정보가 필요한 조회 경로에는 @EntityGraph로 한 번에 가져온다
     * (UserAccountRepository.findAllByUserIdOrderByIdAsc 참고).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id")
    private Bank bank;

    /** 예금주명 */
    @Column(name = "account_owner", nullable = false, length = 50)
    private String accountOwner;

    /** 계좌번호 */
    @Convert(converter = AesGcmConverter.class)
    @Column(name = "account", nullable = false, length = 255)
    private String account;

    @Builder
    private UserAccount(Long userId, BankName bankName, Bank bank, String accountOwner, String account) {
        this.userId = userId;
        this.bankName = bankName;
        this.bank = bank;
        this.accountOwner = accountOwner;
        this.account = account;
    }

    /**
     * null인 필드는 건드리지 않는 부분 수정(PATCH) — 트랜잭션 커밋 시 dirty checking으로 UPDATE된다.
     * bankName과 bank는 항상 같은 은행을 가리켜야 하므로 함께 넘기고 함께 바꾼다
     * (둘 중 하나만 바뀌면 표시되는 아이콘과 실제 은행이 어긋난다).
     */
    public void update(BankName bankName, Bank bank, String accountOwner, String account) {
        if (bankName != null) {
            this.bankName = bankName;
            this.bank = bank;
        }
        if (accountOwner != null) {
            this.accountOwner = accountOwner;
        }
        if (account != null) {
            this.account = account;
        }
    }

    /** 아이콘 URL — 백필 전 레거시 행은 bank가 null이므로 널가드가 필요하다 */
    public String getBankIconUrl() {
        return bank == null ? null : bank.getIconUrl();
    }

    /** 표시명 — bank가 없으면 enum의 기본 표시명으로 대체한다(백필 전에도 화면이 비지 않도록) */
    public String getBankDisplayName() {
        return bank == null ? bankName.getDisplayName() : bank.getDisplayName();
    }

    /** 소유권 검사 — 서비스 계층에서 남의 계좌 접근(IDOR)을 막을 때 사용 */
    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }
}
