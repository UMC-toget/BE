package com.example.toget.domain.gift.entity;

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
}
