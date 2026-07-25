package com.example.toget.domain.gift.service;

import com.example.toget.domain.gift.converter.ProductConverter;
import com.example.toget.domain.gift.dto.request.ProductCreateRequest;
import com.example.toget.domain.gift.dto.request.ProductUpdateRequest;
import com.example.toget.domain.gift.dto.response.ProductCreateResponse;
import com.example.toget.domain.gift.dto.response.ProductDetailResponse;
import com.example.toget.domain.gift.dto.response.ProductListResponse;
import com.example.toget.domain.gift.entity.Product;
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

    private final ProductRepository productRepository;

    @Transactional
    public ProductCreateResponse createProduct(ProductCreateRequest request) {
        Product product = ProductConverter.toEntity(request);
        Product savedProduct = productRepository.save(product);
        return ProductConverter.toCreateResponse(savedProduct);
    }

    public ProductDetailResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));
        return ProductConverter.toDetailResponse(product);
    }

    public ProductListResponse getProducts(String category, String keyword, String brand, Long minPrice, Long maxPrice, int page, int size, String sort) {
        Sort sortOrder = Sort.by(Sort.Direction.DESC, "id");
        if ("oldest".equalsIgnoreCase(sort) || "asc".equalsIgnoreCase(sort)) {
            sortOrder = Sort.by(Sort.Direction.ASC, "id");
        } else if ("price_asc".equalsIgnoreCase(sort)) {
            sortOrder = Sort.by(Sort.Direction.ASC, "price").and(Sort.by(Sort.Direction.DESC, "id"));
        } else if ("price_desc".equalsIgnoreCase(sort)) {
            sortOrder = Sort.by(Sort.Direction.DESC, "price").and(Sort.by(Sort.Direction.DESC, "id"));
        }

        Pageable pageable = PageRequest.of(page, size, sortOrder);

        String formattedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String formattedCategory = (category != null && !category.isBlank()) ? category.trim() : null;
        String formattedBrand = (brand != null && !brand.isBlank()) ? brand.trim() : null;

        Slice<Product> products = productRepository.searchProducts(formattedCategory, formattedKeyword, formattedBrand, minPrice, maxPrice, pageable);
        return ProductConverter.toListResponse(products);
    }

    @Transactional
    public ProductDetailResponse updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));

        product.update(
                request.name(),
                request.price(),
                request.description(),
                request.imageUrl(),
                request.purchaseUrl(),
                request.category(),
                request.brand()
        );

        return ProductConverter.toDetailResponse(product);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));

        productRepository.delete(product);
    }
}
