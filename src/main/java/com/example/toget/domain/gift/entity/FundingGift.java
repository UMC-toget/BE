package com.example.toget.domain.gift.entity;

import com.example.toget.domain.gift.enums.FundingGiftStatus;
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

    /** 후보 등록자. TOGETHER_GIFT 후보만 값 존재, MY_GIFT 수령희망 선물은 null */
    @Column(name = "funding_member_id")
    private Long fundingMemberId;

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

    /** TOGETHER_GIFT 후보 등록 — 등록자(fundingMemberId) 필수, CANDIDATE로 시작 */
    public static FundingGift createCandidate(Long fundingId, Long fundingMemberId, String name, Long price,
                                              String purchaseUrl, String imageUrl, String note) {
        return FundingGift.builder()
                .fundingId(fundingId)
                .fundingMemberId(fundingMemberId)
                .name(name)
                .price(price)
                .purchaseUrl(purchaseUrl)
                .imageUrl(imageUrl)
                .status(FundingGiftStatus.CANDIDATE)
                .note(note)
                .build();
    }

    /** MY_GIFT 수령희망 선물 등록 — 등록자 개념 없음(null), 즉시 SELECTED */
    public static FundingGift createWishGift(Long fundingId, String name, Long price,
                                             String purchaseUrl, String imageUrl) {
        return FundingGift.builder()
                .fundingId(fundingId)
                .fundingMemberId(null)
                .name(name)
                .price(price)
                .purchaseUrl(purchaseUrl)
                .imageUrl(imageUrl)
                .status(FundingGiftStatus.SELECTED)
                .build();
    }

    /** PUT /gifts upsert 시 기존 항목 필드 갱신용 */
    public void updateWishGiftInfo(String name, Long price, String purchaseUrl, String imageUrl) {
        this.name = name;
        this.price = price;
        this.purchaseUrl = purchaseUrl;
        this.imageUrl = imageUrl;
    }

    /** 최종 선물로 확정 */
    public void select() {
        this.status = FundingGiftStatus.SELECTED;
    }

    /** 확정 취소 (다시 후보로) */
    public void unselect() {
        this.status = FundingGiftStatus.CANDIDATE;
    }
}