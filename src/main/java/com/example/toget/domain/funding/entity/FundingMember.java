package com.example.toget.domain.funding.entity;

import com.example.toget.domain.funding.enums.FundingRole;
import com.example.toget.domain.funding.enums.SettlementStatus;
import com.example.toget.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 펀딩 멤버 엔티티 — funding_members 테이블 매핑. TOGETHER_GIFT 전용.
 *
 * [설계 포인트]
 *  - 펀딩당 유저는 1 row (UNIQUE(funding_id, user_id)). role은 언제든 바뀔 수 있다.
 *  - amountDue/settlementStatus는 "정산 확정" 이벤트(Funding.confirmSettlement) 시점에
 *    함께 세팅되는 값으로, 그 전까지는 둘 다 null.
 *    - amountDue가 null   → 정산 미확정 또는 정산 대상에서 제외됨(선물 투표만 참여)
 *    - amountDue가 not null → 정산 참여자로 확정, 이 금액을 내야 함
 *  - amountDue는 한 번 확정되면(정산 시작 후) 원칙적으로 변경/제외 불가 —
 *    이 규칙은 DB 제약이 아니라 서비스 계층에서 Funding.status를 보고 강제한다.
 *  - 투표 자격(FundingGiftVote)은 이 row의 존재 여부만으로 판단하며, 정산 참여 여부와 무관하다.
 */
@Entity
@Table(
    name = "funding_members",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_funding_member_user",
            columnNames = {"funding_id", "user_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FundingMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "funding_member_id")
    private Long id;

    @Column(name = "funding_id", nullable = false)
    private Long fundingId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private FundingRole role;

    /** 정산 확정 금액. 정산 미확정/제외 시 null */
    @Column(name = "amount_due")
    private Long amountDue;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status", length = 20)
    private SettlementStatus settlementStatus;

    @Builder
    private FundingMember(Long fundingId, Long userId, FundingRole role,
                          Long amountDue, SettlementStatus settlementStatus) {
        this.fundingId = fundingId;
        this.userId = userId;
        this.role = role;
        this.amountDue = amountDue;
        this.settlementStatus = settlementStatus;
    }

    /**
     * 펀딩 개설자 row 생성.
     */
    public static FundingMember createCreator(Long fundingId, Long userId) {
        return FundingMember.builder()
                .fundingId(fundingId)
                .userId(userId)
                .role(FundingRole.CREATOR)
                .build();
    }

    /** 일반 참여자로 펀딩 합류 */
    public static FundingMember createParticipant(Long fundingId, Long userId) {
        return FundingMember.builder()
                .fundingId(fundingId)
                .userId(userId)
                .role(FundingRole.PARTICIPANT)
                .build();
    }

    /** 관리자로 위임 — 개설자만 호출 가능 */
    public void promoteToAdmin() {
        if (this.role == FundingRole.CREATOR) {
            // 개설자는 역할 변경 대상이 아님 — 예외처리 추가
        }
        this.role = FundingRole.ADMIN;
    }

    /** 일반 참여자로 강등 (현재 로직엔 없지만 추후에 필요시 사용) */
    public void demoteToParticipant() {
        if (this.role == FundingRole.CREATOR) {
            // 예외처리 추가
        }
        this.role = FundingRole.PARTICIPANT;
    }

    /**
     * 정산 확정 시 이 멤버를 정산 대상에 포함하며 금액을 배정한다.
     */
    public void confirmSettlement(Long amountDue) {
        this.amountDue = amountDue;
        this.settlementStatus = SettlementStatus.UNPAID;
    }

    /**
     * 정산 입금 상태 변경. 개설자가 언제든 자유롭게 호출 가능
     * (순서를 강제하지 않음 — UNPAID/PAID/CONFIRMED 어느 값으로도 임의 변경 가능).
     */
    public void changeSettlementStatus(SettlementStatus settlementStatus) {
        if (this.amountDue == null) {
            // 정산 대상이 아닌 멤버 — 예외처리 추가
        }
        this.settlementStatus = settlementStatus;
    }

    /** 이 멤버가 정산 대상으로 확정됐는지 여부 */
    public boolean isSettlementTarget() {
        return this.amountDue != null;
    }
}