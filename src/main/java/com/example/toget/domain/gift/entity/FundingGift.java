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
    private Long id;

    @Column(name = "funding_id", nullable = false)
    private Long fundingId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long price;

    @Column(name = "purchase_url")
    private String purchaseUrl;

    @Column(name = "image_url")
    private String imageUrl;
}
