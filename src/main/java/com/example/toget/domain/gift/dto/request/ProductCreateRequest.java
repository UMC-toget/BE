package com.example.toget.domain.gift.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductCreateRequest(
    @Schema(description = "상품명", example = "애플 워치 SE 2세대")
    @NotBlank(message = "상품명은 필수입니다.")
    @Size(max = 100, message = "상품명은 100자 이하이어야 합니다.")
    String name,

    @Schema(description = "가격", example = "329000")
    @NotNull(message = "가격은 필수입니다.")
    @PositiveOrZero(message = "가격은 0원 이상이어야 합니다.")
    Long price,

    @Schema(description = "상품 설명", example = "스마트 워치")
    String description,

    @Schema(description = "상품 이미지 URL", example = "https://example.com/image.jpg")
    String imageUrl,

    @Schema(description = "구매 링크 URL", example = "https://example.com/shop/1")
    @NotBlank(message = "구매 링크 URL은 필수입니다.")
    String purchaseUrl,

    @Schema(description = "카테고리", example = "전자기기")
    @Size(max = 50, message = "카테고리는 50자 이하이어야 합니다.")
    String category,

    @Schema(description = "브랜드", example = "Apple")
    @Size(max = 50, message = "브랜드는 50자 이하이어야 합니다.")
    String brand
) {}
