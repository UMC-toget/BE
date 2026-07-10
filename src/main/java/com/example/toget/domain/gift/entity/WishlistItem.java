package com.example.toget.domain.gift.entity;

import com.example.toget.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wishlist_items")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WishlistItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long price;

    @Column(name = "purchase_url", nullable = true)
    private String purchaseUrl;

    @Column(name = "image_url", nullable = true)
    private String imageUrl;
}
