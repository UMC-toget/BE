package com.example.toget.domain.gift.entity;

import com.example.toget.domain.gift.enums.CategoryType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", nullable = false, length = 30)
    private CategoryType categoryType;

    public ProductCategory(Product product, CategoryType categoryType) {
        this.product = product;
        this.categoryType = categoryType;
    }
}
