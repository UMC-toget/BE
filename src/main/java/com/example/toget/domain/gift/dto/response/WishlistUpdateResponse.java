package com.example.toget.domain.gift.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "위시리스트 아이템 수정 응답 DTO")
public record WishlistUpdateResponse(
    @Schema(description = "수정된 위시리스트 아이템 ID", example = "1")
    Long wishlistItemId,

    @Schema(description = "상품 이름", example = "맥북 프로 14 (M3)")
    String name,

    @Schema(description = "가격", example = "2390000")
    Long price,

    @Schema(description = "구매처 URL", example = "https://apple.com/kr/macbook-pro")
    String purchaseUrl,

    @Schema(description = "상품 이미지 URL", example = "https://image.com/macbook-updated.png")
    String imageUrl
) {
}
