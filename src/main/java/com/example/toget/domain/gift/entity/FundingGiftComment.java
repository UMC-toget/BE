package com.example.toget.domain.gift.entity;

import com.example.toget.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 선물 후보 댓글 엔티티 — funding_gift_comments 테이블 매핑. TOGETHER_GIFT 전용.
 * 요청자가 해당 펀딩의 멤버여야 작성 가능하므로 fundingMemberId를 참조한다
 * (FundingGiftVote와 동일한 참조 방식).
 */
@Entity
@Table(name = "funding_gift_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FundingGiftComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "funding_gift_id", nullable = false)
    private Long fundingGiftId;

    @Column(name = "funding_member_id", nullable = false)
    private Long fundingMemberId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    private FundingGiftComment(Long fundingGiftId, Long fundingMemberId, String content) {
        this.fundingGiftId = fundingGiftId;
        this.fundingMemberId = fundingMemberId;
        this.content = content;
    }

    public static FundingGiftComment create(Long fundingGiftId, Long fundingMemberId, String content) {
        return FundingGiftComment.builder()
                .fundingGiftId(fundingGiftId)
                .fundingMemberId(fundingMemberId)
                .content(content)
                .build();
    }
}