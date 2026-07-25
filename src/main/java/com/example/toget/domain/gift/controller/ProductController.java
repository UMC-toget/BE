package com.example.toget.domain.gift.controller;

import com.example.toget.domain.gift.dto.request.ProductCreateRequest;
import com.example.toget.domain.gift.dto.request.ProductUpdateRequest;
import com.example.toget.domain.gift.dto.response.ProductCreateResponse;
import com.example.toget.domain.gift.dto.response.ProductDetailResponse;
import com.example.toget.domain.gift.dto.response.ProductListResponse;
import com.example.toget.domain.gift.service.ProductService;
import com.example.toget.global.annotation.AdminOnly;
import com.example.toget.global.apiPayload.ApiResponse;
import com.example.toget.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "GIFT / 자체 상품 API", description = "자체 상품 CRUD API")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "상품 목록 조회", description = "자체 상품 목록을 카테고리, 검색어, 페이징 및 정렬 조건에 따라 조회합니다.")
    @GetMapping
    public ApiResponse<ProductListResponse> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, productService.getProducts(category, keyword, page, size, sort));
    }

    @Operation(summary = "상품 상세 조회", description = "지정한 ID의 상품 상세 정보를 조회합니다.")
    @GetMapping("/{productId}")
    public ApiResponse<ProductDetailResponse> getProduct(
            @PathVariable Long productId
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, productService.getProduct(productId));
    }

    @AdminOnly
    @Operation(summary = "[관리자 전용] 상품 등록", description = "새로운 자체 상품을 등록합니다.")
    @PostMapping
    public ApiResponse<ProductCreateResponse> createProduct(
            @Valid @RequestBody ProductCreateRequest request
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, productService.createProduct(request));
    }

    @AdminOnly
    @Operation(summary = "[관리자 전용] 상품 수정", description = "지정한 ID의 상품 정보를 수정합니다.")
    @PutMapping("/{productId}")
    public ApiResponse<ProductDetailResponse> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, productService.updateProduct(productId, request));
    }

    @AdminOnly
    @Operation(summary = "[관리자 전용] 상품 삭제", description = "지정한 ID의 상품을 삭제합니다.")
    @DeleteMapping("/{productId}")
    public ApiResponse<Void> deleteProduct(
            @PathVariable Long productId
    ) {
        productService.deleteProduct(productId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }
}
