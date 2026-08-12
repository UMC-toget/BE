package com.example.toget.domain.gift.controller;

import com.example.toget.domain.gift.dto.request.ProductCreateRequest;
import com.example.toget.domain.gift.dto.request.ProductUpdateRequest;
import com.example.toget.domain.gift.dto.response.ProductCreateResponse;
import com.example.toget.domain.gift.dto.response.ProductDetailResponse;
import com.example.toget.domain.gift.service.ProductService;
import com.example.toget.global.annotation.AdminOnly;
import com.example.toget.global.apiPayload.ApiResponse;
import com.example.toget.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "ADMIN / 상품 관리 API", description = "관리자 전용 상품 CRUD API")
@Validated
@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@AdminOnly
public class AdminProductController {

    private final ProductService productService;

    @Operation(summary = "[관리자 전용] 상품 등록", description = "새로운 자체 상품을 등록합니다. (다중 카테고리 지정 가능)")
    @PostMapping
    public ApiResponse<ProductCreateResponse> createProduct(
            @Valid @RequestBody ProductCreateRequest request
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, productService.createProduct(request));
    }

    @Operation(summary = "[관리자 전용] 상품 수정", description = "지정한 ID의 상품 정보를 수정합니다. (다중 카테고리 지정 가능)")
    @PutMapping("/{productId}")
    public ApiResponse<ProductDetailResponse> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, productService.updateProduct(productId, request));
    }

    @Operation(summary = "[관리자 전용] 상품 삭제", description = "지정한 ID의 상품을 삭제합니다.")
    @DeleteMapping("/{productId}")
    public ApiResponse<Void> deleteProduct(
            @PathVariable Long productId
    ) {
        productService.deleteProduct(productId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }
}
