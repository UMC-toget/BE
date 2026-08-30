package com.example.toget.domain.gift.service;

import com.example.toget.domain.gift.converter.ProductConverter;
import com.example.toget.domain.gift.dto.request.ProductCreateRequest;
import com.example.toget.domain.gift.dto.request.ProductUpdateRequest;
import com.example.toget.domain.gift.dto.response.ProductCreateResponse;
import com.example.toget.domain.gift.dto.response.ProductDetailResponse;
import com.example.toget.domain.gift.dto.response.ProductListResponse;
import com.example.toget.domain.gift.entity.Product;
import com.example.toget.domain.gift.enums.CategoryType;
import com.example.toget.domain.gift.enums.ProductSort;
import com.example.toget.domain.gift.exception.ProductException;
import com.example.toget.domain.gift.exception.code.ProductErrorCode;
import com.example.toget.domain.gift.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository productRepository;

    @Transactional
    public ProductCreateResponse createProduct(ProductCreateRequest request) {
        Product product = ProductConverter.toEntity(request);
        Product savedProduct = productRepository.save(product);
        return ProductConverter.toCreateResponse(savedProduct);
    }

    public ProductDetailResponse getProduct(Long productId) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));
        return ProductConverter.toDetailResponse(product);
    }

    public ProductListResponse getProducts(String category, String keyword, String brand, Long minPrice, Long maxPrice, int page, int size, ProductSort sort) {
        int safePage = Math.max(page, 0);
        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        ProductSort sortType = (sort != null) ? sort : ProductSort.LATEST;
        Sort sortOrder = switch (sortType) {
            case OLDEST -> Sort.by(Sort.Direction.ASC, "id");
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "price").and(Sort.by(Sort.Direction.DESC, "id"));
            case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "price").and(Sort.by(Sort.Direction.DESC, "id"));
            // 위시리스트 등록 횟수 내림차순. 동점 시 최신 상품이 앞에 오도록 id DESC로 tie-break한다.
            case WISHLIST_DESC -> Sort.by(Sort.Direction.DESC, "wishlistCount").and(Sort.by(Sort.Direction.DESC, "id"));
            case LATEST -> Sort.by(Sort.Direction.DESC, "id");
        };

        Pageable pageable = PageRequest.of(safePage, safeSize, sortOrder);

        String formattedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        CategoryType categoryEnum = (category != null && !category.isBlank()) ? CategoryType.from(category) : null;
        String formattedBrand = (brand != null && !brand.isBlank()) ? brand.trim() : null;

        Slice<Product> products = productRepository.searchProducts(categoryEnum, formattedKeyword, formattedBrand, minPrice, maxPrice, pageable);
        return ProductConverter.toListResponse(products);
    }

    @Transactional
    public ProductDetailResponse updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));

        product.update(
                request.name(),
                request.price(),
                request.description(),
                request.imageUrl(),
                request.purchaseUrl(),
                request.categoryTypes(),
                request.brand()
        );

        return ProductConverter.toDetailResponse(product);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));

        product.delete();
    }
}
