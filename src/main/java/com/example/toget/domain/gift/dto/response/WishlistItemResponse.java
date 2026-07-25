package com.example.toget.domain.gift.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "위시리스트 아이템 조회 응답 DTO")
public record WishlistItemResponse(
    @Schema(description = "위시리스트 아이템 ID", example = "1")
    Long wishlistItemId,

    @Schema(description = "상품 이름", example = "맥북 프로 14")
    String name,

    @Schema(description = "가격", example = "2490000")
    Long price,

    @Schema(description = "구매처 URL", example = "https://apple.com/kr/macbook-pro")
    String purchaseUrl,

    @Schema(description = "상품 이미지 URL", example = "https://image.com/macbook.png")
    String imageUrl,

    @Schema(description = "생성 일시", example = "2026-07-05T01:28:20")
    LocalDateTime createdAt
) {
}
