package com.example.toget.domain.gift.entity;

import com.example.toget.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long price;

    @Column
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "purchase_url", nullable = false)
    private String purchaseUrl;

    @Column
    private String category;

    @Column
    private String brand;
}
