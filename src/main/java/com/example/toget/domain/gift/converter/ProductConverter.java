package com.example.toget.domain.gift.converter;

import com.example.toget.domain.gift.dto.request.ProductCreateRequest;
import com.example.toget.domain.gift.dto.response.ProductCreateResponse;
import com.example.toget.domain.gift.dto.response.ProductDetailResponse;
import com.example.toget.domain.gift.dto.response.ProductListResponse;
import com.example.toget.domain.gift.entity.Product;
import org.springframework.data.domain.Slice;

import java.util.List;

public class ProductConverter {

    private ProductConverter() {
    }

    public static Product toEntity(ProductCreateRequest request) {
        return Product.builder()
                .name(request.name())
                .price(request.price())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .purchaseUrl(request.purchaseUrl())
                .category(request.category())
                .brand(request.brand())
                .build();
    }

    public static ProductCreateResponse toCreateResponse(Product product) {
        return new ProductCreateResponse(product.getId());
    }

    public static ProductDetailResponse toDetailResponse(Product product) {
        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getDescription(),
                product.getImageUrl(),
                product.getPurchaseUrl(),
                product.getCategory(),
                product.getBrand(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public static ProductListResponse toListResponse(Slice<Product> slice) {
        List<ProductDetailResponse> items = slice.getContent().stream()
                .map(ProductConverter::toDetailResponse)
                .toList();

        return new ProductListResponse(
                items,
                slice.getNumber(),
                slice.getSize(),
                slice.hasNext()
        );
    }
}
