package com.example.toget.domain.gift.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "위시리스트 아이템 생성 응답 DTO")
public record WishlistCreateResponse(
    @Schema(description = "생성된 위시리스트 아이템 ID", example = "1")
    Long wishlistItemId,

    @Schema(description = "매핑된 자체 상품 ID. 외부 링크 상품이면 null", example = "1", nullable = true)
    Long productId
) {
}
