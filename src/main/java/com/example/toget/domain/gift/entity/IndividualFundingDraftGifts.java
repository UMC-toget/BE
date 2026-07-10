package com.example.toget.domain.gift.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "individual_funding_draft_gifts")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class IndividualFundingDraftGifts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "my_drafts_id", nullable = false)
    private Long myDraftId;

    @Column(name = "gift_name", length = 50)
    private String name;

    @Column(name = "gift_price")
    private Long price;

    @Column(name = "gift_shop_url", columnDefinition = "TEXT")
    private String purchaseUrl;

    @Column(name = "gift_image_url", columnDefinition = "TEXT")
    private String imageUrl;
}
