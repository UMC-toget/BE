package com.example.toget.domain.gift.dto.response;

import com.example.toget.domain.gift.enums.CategoryType;

import java.time.LocalDateTime;
import java.util.List;

public record ProductDetailResponse(
    Long productId,
    String name,
    Long price,
    String description,
    String imageUrl,
    String purchaseUrl,
    List<CategoryType> categoryTypes,
    String brand,
    // 위시리스트 등록 횟수 (등록 건수 기준, 등록한 사용자 수가 아님)
    Long wishlistCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
