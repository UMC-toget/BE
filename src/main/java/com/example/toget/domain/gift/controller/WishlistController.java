package com.example.toget.domain.gift.controller;

import com.example.toget.domain.gift.enums.WishlistSort;
import com.example.toget.domain.gift.enums.WishlistType;
import com.example.toget.domain.gift.dto.request.WishlistCreateRequest;
import com.example.toget.domain.gift.dto.request.WishlistUpdateRequest;
import com.example.toget.domain.gift.dto.response.WishlistCreateResponse;
import com.example.toget.domain.gift.dto.response.WishlistListResponse;
import com.example.toget.domain.gift.dto.response.WishlistUpdateResponse;
import com.example.toget.domain.gift.service.WishlistService;
import com.example.toget.domain.user.controller.LoginUserId;
import com.example.toget.global.apiPayload.ApiResponse;
import com.example.toget.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "GIFT / 위시리스트 API", description = "선물 위시리스트 CRUD API")
@Validated
@RestController
@RequestMapping("/api/v1/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @Operation(summary = "위시리스트 조회", description = "현재 로그인한 사용자의 위시리스트 항목 목록을 유형(GIVE/RECEIVE), 페이징 및 정렬(latest/oldest) 조회합니다.")
    @GetMapping
    public ApiResponse<WishlistListResponse> getWishlist(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @Parameter(description = "선물 유형 필터 (GIVE: 주고싶은 선물, RECEIVE: 받고싶은 선물)") @RequestParam(required = false) WishlistType type,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "LATEST") WishlistSort sort
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, wishlistService.getWishlist(userId, type, page, size, sort));
    }

    @Operation(summary = "위시리스트 아이템 생성",
            description = "현재 로그인한 사용자의 새로운 선물 위시리스트 항목을 생성합니다. "
                    + "productId를 지정하면 해당 자체 상품의 위시리스트 등록 횟수가 1 증가합니다. "
                    + "같은 상품이라도 GIVE와 RECEIVE는 각각 등록할 수 있으며, 이 경우 등록 횟수는 총 2 증가합니다. "
                    + "다만 이미 등록한 유형으로 같은 상품을 다시 등록하면 409를 반환합니다.")
    @PostMapping
    public ApiResponse<WishlistCreateResponse> createWishlistItem(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @Valid @RequestBody WishlistCreateRequest request
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, wishlistService.create(userId, request));
    }

    @Operation(summary = "위시리스트 아이템 수정",
            description = "지정한 ID의 위시리스트 항목 정보를 수정합니다. "
                    + "매핑된 상품(productId)은 수정할 수 없으며, 변경이 필요하면 삭제 후 다시 등록해야 합니다.")
    @PutMapping("/{wishlistItemId}")
    public ApiResponse<WishlistUpdateResponse> updateWishlistItem(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long wishlistItemId,
            @Valid @RequestBody WishlistUpdateRequest request
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, wishlistService.update(userId, wishlistItemId, request));
    }

    @Operation(summary = "위시리스트 아이템 삭제", description = "지정한 ID의 위시리스트 항목을 삭제합니다.")
    @DeleteMapping("/{wishlistItemId}")
    public ApiResponse<Void> deleteWishlistItem(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @PathVariable Long wishlistItemId
    ) {
        wishlistService.delete(userId, wishlistItemId);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, null);
    }
}
