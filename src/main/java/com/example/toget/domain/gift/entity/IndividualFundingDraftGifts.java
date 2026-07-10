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

    @Column(name = "my_draft_id", nullable = false)
    private Long myDraftId;

    @Column
    private String name;

    @Column
    private Long price;

    @Column(name = "purchase_url")
    private String purchaseUrl;

    @Column(name = "image_url")
    private String imageUrl;
}
