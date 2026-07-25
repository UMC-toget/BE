package com.example.toget.domain.gift.controller;

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
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "GIFT / 위시리스트 API", description = "선물 위시리스트 CRUD API")
@RestController
@RequestMapping("/api/v1/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @Operation(summary = "위시리스트 조회", description = "현재 로그인한 사용자의 위시리스트 항목 목록을 페이징 및 정렬(latest/oldest) 조회합니다.")
    @GetMapping
    public ApiResponse<WishlistListResponse> getWishlist(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, wishlistService.getWishlist(userId, page, size, sort));
    }

    @Operation(summary = "위시리스트 아이템 생성", description = "현재 로그인한 사용자의 새로운 선물 위시리스트 항목을 생성합니다.")
    @PostMapping
    public ApiResponse<WishlistCreateResponse> createWishlistItem(
            @Parameter(hidden = true) @LoginUserId Long userId,
            @Valid @RequestBody WishlistCreateRequest request
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, wishlistService.create(userId, request));
    }

    @Operation(summary = "위시리스트 아이템 수정", description = "지정한 ID의 위시리스트 항목 정보를 수정합니다.")
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
