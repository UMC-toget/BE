package com.example.toget.domain.gift.dto.response;

import java.util.List;

public record ProductListResponse(
    List<ProductDetailResponse> products,
    int currentPage,
    int pageSize,
    boolean hasNext
) {}

