package com.example.toget.domain.funding.entity;

import com.example.toget.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 후보 선물 투표 엔티티 — TOGETHER_GIFT 전용.
 *
 * [설계 포인트]
 *  - 멤버당 투표 1회 (UNIQUE(funding_member_id)). 다른 후보로 바꾸고 싶으면
 *    기존 row를 삭제하고 새로 생성하거나, changeVote(...)로 funding_gift_id를 갱신한다.
 *  - 투표 자격은 FundingMember 존재 여부로만 판단한다 (정산 참여 여부와 무관).
 */
@Entity
@Table(
     name = "funding_gift_votes",
     uniqueConstraints = {
         @UniqueConstraint(
             name = "uk_member_gift_vote",
             columnNames = {"funding_member_id", "funding_gift_id"}
         )
     }
 )
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FundingGiftVote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "funding_gift_vote_id")
    private Long id;

    @Column(name = "funding_gift_id", nullable = false)
    private Long fundingGiftId;

    @Column(name = "funding_member_id", nullable = false)
    private Long fundingMemberId;

    @Builder
    private FundingGiftVote(Long fundingGiftId, Long fundingMemberId) {
        this.fundingGiftId = fundingGiftId;
        this.fundingMemberId = fundingMemberId;
    }

    public static FundingGiftVote create(Long fundingGiftId, Long fundingMemberId) {
        return FundingGiftVote.builder()
                .fundingGiftId(fundingGiftId)
                .fundingMemberId(fundingMemberId)
                .build();
    }

    /** 투표 대상 변경 (다른 후보 선물로 재투표) */
    public void changeVote(Long newFundingGiftId) {
        this.fundingGiftId = newFundingGiftId;
    }
}