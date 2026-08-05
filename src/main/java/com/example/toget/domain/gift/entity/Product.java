package com.example.toget.domain.gift.entity;

import com.example.toget.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "price", nullable = false)
    private Long price;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "shop_url", nullable = false, columnDefinition = "TEXT")
    private String purchaseUrl;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "brand", length = 50)
    private String brand;

    /**
     * 위시리스트 등록 횟수. 인기 상품 정렬(WISHLIST_DESC)의 기준값.
     *
     * [설계 포인트]
     *  - 등록 "건수(행)" 기준이며 등록한 사용자 수가 아니다.
     *    한 유저가 같은 상품을 GIVE/RECEIVE로 모두 담으면 +2 된다.
     *  - 증감은 이 필드를 직접 수정하지 않고 ProductRepository의 원자적 UPDATE 쿼리로만 처리한다.
     */
    @Column(name = "wishlist_count", nullable = false, columnDefinition = "BIGINT NOT NULL DEFAULT 0")
    @Builder.Default
    private Long wishlistCount = 0L;

    public void update(String name, Long price, String description, String imageUrl, String purchaseUrl, String category, String brand) {
        this.name = name;
        this.price = price;
        this.description = description;
        this.imageUrl = imageUrl;
        this.purchaseUrl = purchaseUrl;
        this.category = category;
        this.brand = brand;
    }

    public void delete() {
        this.deletedAt = java.time.LocalDateTime.now();
    }
}
