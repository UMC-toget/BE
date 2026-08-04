package com.example.toget.domain.gift.converter;

import com.example.toget.domain.gift.dto.request.WishlistCreateRequest;
import com.example.toget.domain.gift.dto.response.WishlistCreateResponse;
import com.example.toget.domain.gift.dto.response.WishlistItemResponse;
import com.example.toget.domain.gift.dto.response.WishlistListResponse;
import com.example.toget.domain.gift.dto.response.WishlistUpdateResponse;
import com.example.toget.domain.gift.entity.WishlistItem;
import org.springframework.data.domain.Slice;

import java.util.List;

public class WishlistConverter {

    private WishlistConverter() {
    }

    public static WishlistItem toEntity(Long userId, WishlistCreateRequest request) {
        return WishlistItem.builder()
                .userId(userId)
                .productId(request.productId())
                .name(request.name())
                .price(request.price())
                .purchaseUrl(request.purchaseUrl())
                .imageUrl(request.imageUrl())
                .type(request.type())
                .build();
    }

    public static WishlistCreateResponse toCreateResponse(WishlistItem item) {
        return new WishlistCreateResponse(item.getId(), item.getProductId());
    }

    public static WishlistItemResponse toItemResponse(WishlistItem item) {
        return new WishlistItemResponse(
                item.getId(),
                item.getProductId(),
                item.getName(),
                item.getPrice(),
                item.getPurchaseUrl(),
                item.getImageUrl(),
                item.getType(),
                item.getCreatedAt()
        );
    }

    public static WishlistListResponse toListResponse(Slice<WishlistItem> slice) {
        List<WishlistItemResponse> items = slice.getContent().stream()
                .map(WishlistConverter::toItemResponse)
                .toList();

        return new WishlistListResponse(
                items,
                slice.getNumber(),
                slice.getSize(),
                slice.hasNext()
        );
    }

    public static WishlistUpdateResponse toUpdateResponse(WishlistItem item) {
        return new WishlistUpdateResponse(
                item.getId(),
                item.getName(),
                item.getPrice(),
                item.getPurchaseUrl(),
                item.getImageUrl(),
                item.getType()
        );
    }
}
