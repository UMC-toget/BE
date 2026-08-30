package com.example.toget.domain.gift.entity;

import com.example.toget.domain.gift.enums.WishlistType;
import com.example.toget.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 위시리스트 아이템 엔티티 — wishlist_items 테이블 매핑.
 *
 * [설계 포인트]
 *  - 같은 유저가 같은 상품을 GIVE와 RECEIVE로 각각 1건씩 담는 것은 허용하고,
 *    동일 타입 중복 등록만 DB 유니크 제약(user_id, product_id, type)으로 차단한다.
 *  - product_id가 NULL인 항목(외부 링크 상품)은 MySQL 유니크 제약 특성상 검사 대상에서 제외되므로
 *    중복 제한이 적용되지 않는다. 자유 입력 항목이므로 의도된 동작이다.
 *  - 중복 검사는 서비스 계층에서 선조회로 수행하고, 유니크 제약은 동시 요청 대비 2차 방어선이다.
 */
@Entity
@Table(
        name = "wishlist_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_product_type",
                        columnNames = {"user_id", "product_id", "type"}
                )
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WishlistItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wishlist_item_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 매핑된 자체 상품 ID. 외부 링크 상품 등 자체 상품이 아닌 경우 null.
     *
     * [설계 포인트]
     *  - 기존과 동일하게 JPA 연관관계(@ManyToOne) 없이 Long ID로만 보관한다.
     *    따라서 DB FK 제약은 생성되지 않으며, 상품 존재 여부는 서비스 계층에서 검증한다.
     *  - 항목의 정체성을 결정하는 값이므로 생성 시점에 고정되며 update()로 변경할 수 없다.
     *    매핑 변경이 필요하면 삭제 후 재등록으로 처리한다.
     */
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "price", nullable = false)
    private Long price;

    @Column(name = "purchase_url", columnDefinition = "TEXT")
    private String purchaseUrl;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private WishlistType type;

    /** productId는 의도적으로 수정 대상에서 제외한다 (위 설계 포인트 참고). */
    public void update(String name, Long price, String purchaseUrl, String imageUrl, WishlistType type) {
        this.name = name;
        this.price = price;
        this.purchaseUrl = purchaseUrl;
        this.imageUrl = imageUrl;
        this.type = type;
    }
}
