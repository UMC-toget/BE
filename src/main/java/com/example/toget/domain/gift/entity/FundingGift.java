package com.example.toget.domain.gift.entity;

import com.example.toget.domain.funding.enums.FundingGiftStatus;
import com.example.toget.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "funding_gifts")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FundingGift extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "funding_gift_id")
    private Long id;

    @Column(name = "funding_id", nullable = false)
    private Long fundingId;

    @Column(name = "gift_name", nullable = false, length = 100)
    private String name;

    @Column(name = "gift_price", nullable = false)
    private Long price;

    @Column(name = "gift_purchase_url", columnDefinition = "TEXT")
    private String purchaseUrl;

    @Column(name = "gift_image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private FundingGiftStatus status = FundingGiftStatus.CANDIDATE;

    @Column(name = "note", length = 100)
    private String note;

    /** 최종 선물로 확정 */
    public void select() {
        this.status = FundingGiftStatus.SELECTED;
    }

    /** 확정 취소 (다시 후보로) */
    public void unselect() {
        this.status = FundingGiftStatus.CANDIDATE;
    }
}
