package com.example.toget.domain.gift.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "위시리스트 아이템 수정 요청 DTO")
public record WishlistUpdateRequest(
    @Schema(description = "상품 이름", example = "맥북 프로 14 (M3)")
    @NotBlank(message = "상품 이름은 필수입니다.")
    @Size(max = 100, message = "상품 이름은 100자 이하이어야 합니다.")
    String name,

    @Schema(description = "가격", example = "2390000")
    @NotNull(message = "가격은 필수입니다.")
    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    Long price,

    @Schema(description = "구매처 URL", example = "https://apple.com/kr/macbook-pro")
    @NotBlank(message = "구매 링크는 필수입니다.")
    String purchaseUrl,

    @Schema(description = "상품 이미지 URL", example = "https://image.com/macbook-updated.png")
    String imageUrl
) {
}
