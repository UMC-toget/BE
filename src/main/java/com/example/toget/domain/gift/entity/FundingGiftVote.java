package com.example.toget.domain.gift.entity;

import com.example.toget.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 후보 선물 투표 엔티티 — funding_gift_votes 테이블 매핑. TOGETHER_GIFT 전용.
 *
 * [설계 포인트]
 *  - 1인 최대 3개 후보까지 투표 가능.
 *  - 같은 선물에 중복 투표는 DB 유니크 제약(funding_member_id, funding_gift_id)으로 방지.
 *  - "3표 초과 금지"는 DB로 강제하지 않고 서비스 계층에서 카운트 검증 후 INSERT.
 *  - 투표 취소는 row를 삭제하는 방식으로 처리 (재사용/갱신 없음).
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

    /**
     * 특정 후보 선물에 투표 생성.
     * 호출 전 서비스 계층에서 반드시 다음을 검증해야 한다:
     *  1) 해당 멤버의 기존 투표 수 < 3
     *  2) 같은 funding_gift_id에 이미 투표하지 않았는지 (DB 유니크 제약으로도 이중 방지)
     */
    public static FundingGiftVote create(Long fundingGiftId, Long fundingMemberId) {
        return FundingGiftVote.builder()
                .fundingGiftId(fundingGiftId)
                .fundingMemberId(fundingMemberId)
                .build();
    }
}