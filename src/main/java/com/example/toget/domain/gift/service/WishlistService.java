package com.example.toget.domain.gift.service;

import com.example.toget.domain.gift.converter.WishlistConverter;
import com.example.toget.domain.gift.dto.request.WishlistCreateRequest;
import com.example.toget.domain.gift.dto.request.WishlistUpdateRequest;
import com.example.toget.domain.gift.dto.response.WishlistCreateResponse;
import com.example.toget.domain.gift.dto.response.WishlistListResponse;
import com.example.toget.domain.gift.dto.response.WishlistUpdateResponse;
import com.example.toget.domain.gift.entity.WishlistItem;
import com.example.toget.domain.gift.exception.WishlistException;
import com.example.toget.domain.gift.exception.code.WishlistErrorCode;
import com.example.toget.domain.gift.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.toget.domain.gift.enums.WishlistSort;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;

    @Transactional
    public WishlistCreateResponse create(Long userId, WishlistCreateRequest request) {
        WishlistItem wishlistItem = WishlistConverter.toEntity(userId, request);
        WishlistItem savedItem = wishlistItemRepository.save(wishlistItem);
        return WishlistConverter.toCreateResponse(savedItem);
    }

    public WishlistListResponse getWishlist(Long userId, int page, int size, WishlistSort sort) {
        Pageable pageable = PageRequest.of(page, size);
        Slice<WishlistItem> slice;
        WishlistSort sortType = (sort != null) ? sort : WishlistSort.LATEST;
        if (sortType == WishlistSort.OLDEST) {
            slice = wishlistItemRepository.findByUserIdOrderByIdAsc(userId, pageable);
        } else {
            slice = wishlistItemRepository.findByUserIdOrderByIdDesc(userId, pageable);
        }
        return WishlistConverter.toListResponse(slice);
    }

    @Transactional
    public WishlistUpdateResponse update(Long userId, Long wishlistItemId, WishlistUpdateRequest request) {
        WishlistItem item = wishlistItemRepository.findById(wishlistItemId)
                .orElseThrow(() -> new WishlistException(WishlistErrorCode.WISHLIST_NOT_FOUND));

        if (!item.getUserId().equals(userId)) {
            throw new WishlistException(WishlistErrorCode.WISHLIST_NOT_OWNER);
        }

        item.update(request.name(), request.price(), request.purchaseUrl(), request.imageUrl());
        return WishlistConverter.toUpdateResponse(item);
    }

    @Transactional
    public void delete(Long userId, Long wishlistItemId) {
        WishlistItem item = wishlistItemRepository.findById(wishlistItemId)
                .orElseThrow(() -> new WishlistException(WishlistErrorCode.WISHLIST_NOT_FOUND));

        if (!item.getUserId().equals(userId)) {
            throw new WishlistException(WishlistErrorCode.WISHLIST_NOT_OWNER);
        }

        wishlistItemRepository.delete(item);
    }
}
