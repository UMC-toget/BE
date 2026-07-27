package com.example.toget.domain.gift.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "위시리스트 목록 조회 응답 DTO")
public record WishlistListResponse(
    @Schema(description = "위시리스트 아이템 목록")
    List<WishlistItemResponse> wishlistItems,

    @Schema(description = "현재 페이지 번호", example = "0")
    int currentPage,

    @Schema(description = "페이지 크기", example = "10")
    int pageSize,

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    boolean hasNext
) {
}
