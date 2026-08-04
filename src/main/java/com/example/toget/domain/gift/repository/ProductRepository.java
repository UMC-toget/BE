package com.example.toget.domain.gift.repository;

import com.example.toget.domain.gift.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT p FROM Product p WHERE " +
           "p.deletedAt IS NULL AND " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:brand IS NULL OR p.brand = :brand) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR (p.brand IS NOT NULL AND LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%'))))")
    Slice<Product> searchProducts(
            @Param("category") String category,
            @Param("keyword") String keyword,
            @Param("brand") String brand,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            Pageable pageable
    );

    /**
     * 위시리스트 등록 횟수 +1.
     *
     * [설계 포인트]
     *  - 엔티티 더티 체킹(조회 → 자바에서 +1 → 커밋)은 읽기와 쓰기 사이에 간격이 있어
     *    동시 요청 시 갱신 유실(lost update)이 발생한다. 따라서 DB가 행 락을 잡고
     *    한 번에 처리하는 원자적 UPDATE로 증감한다.
     *  - flushAutomatically: 영속성 컨텍스트에 남아있는 변경(예: 삭제)을 이 쿼리보다 먼저 DB에 반영한다.
     *  - clearAutomatically: 이 쿼리는 영속성 컨텍스트를 우회하므로, 실행 후 컨텍스트를 비워
     *    이미 로딩된 Product의 낡은 wishlistCount가 재사용되지 않게 한다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Product p SET p.wishlistCount = p.wishlistCount + 1 WHERE p.id = :productId")
    void increaseWishlistCount(@Param("productId") Long productId);

    /** 위시리스트 등록 횟수 -1. wishlistCount > 0 조건이 음수 진입을 막는 안전장치다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Product p SET p.wishlistCount = p.wishlistCount - 1 " +
           "WHERE p.id = :productId AND p.wishlistCount > 0")
    void decreaseWishlistCount(@Param("productId") Long productId);
}
