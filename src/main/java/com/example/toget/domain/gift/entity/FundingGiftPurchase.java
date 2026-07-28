package com.example.toget.domain.gift.entity;

import com.example.toget.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "funding_gift_purchases", uniqueConstraints = {
        @UniqueConstraint(name = "uk_gift_purchase", columnNames = {"funding_gift_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FundingGiftPurchase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "funding_gift_id", nullable = false)
    private Long fundingGiftId;

    @Column(name = "purchase_url", nullable = false, columnDefinition = "TEXT")
    private String purchaseUrl;

    @Column(name = "receipt_image_url", nullable = false, columnDefinition = "TEXT")
    private String receiptImageUrl;

    @Builder
    private FundingGiftPurchase(Long fundingGiftId, String purchaseUrl, String receiptImageUrl) {
        this.fundingGiftId = fundingGiftId;
        this.purchaseUrl = purchaseUrl;
        this.receiptImageUrl = receiptImageUrl;
    }

    public static FundingGiftPurchase create(Long fundingGiftId, String purchaseUrl, String receiptImageUrl) {
        return FundingGiftPurchase.builder()
                .fundingGiftId(fundingGiftId)
                .purchaseUrl(purchaseUrl)
                .receiptImageUrl(receiptImageUrl)
                .build();
    }
}