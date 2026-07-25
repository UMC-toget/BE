package com.example.toget.domain.gift.dto.response;

import java.time.LocalDateTime;

public record ProductDetailResponse(
    Long productId,
    String name,
    Long price,
    String description,
    String imageUrl,
    String purchaseUrl,
    String category,
    String brand,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
