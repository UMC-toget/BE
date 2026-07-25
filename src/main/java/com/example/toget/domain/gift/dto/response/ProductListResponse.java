package com.example.toget.domain.gift.dto.response;

import java.util.List;

public record ProductListResponse(
    List<ProductDetailResponse> products,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean isFirst,
    boolean isLast
) {}
