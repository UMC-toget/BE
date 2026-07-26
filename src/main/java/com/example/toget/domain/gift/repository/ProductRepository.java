package com.example.toget.domain.gift.repository;

import com.example.toget.domain.gift.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
