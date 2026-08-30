package com.example.toget.domain.gift.service;

import com.example.toget.domain.gift.converter.WishlistConverter;
import com.example.toget.domain.gift.dto.request.WishlistCreateRequest;
import com.example.toget.domain.gift.dto.request.WishlistUpdateRequest;
import com.example.toget.domain.gift.dto.response.WishlistCreateResponse;
import com.example.toget.domain.gift.dto.response.WishlistListResponse;
import com.example.toget.domain.gift.dto.response.WishlistUpdateResponse;
import com.example.toget.domain.gift.entity.WishlistItem;
import com.example.toget.domain.gift.enums.WishlistSort;
import com.example.toget.domain.gift.enums.WishlistType;
import com.example.toget.domain.gift.exception.ProductException;
import com.example.toget.domain.gift.exception.WishlistException;
import com.example.toget.domain.gift.exception.code.ProductErrorCode;
import com.example.toget.domain.gift.exception.code.WishlistErrorCode;
import com.example.toget.domain.gift.repository.ProductRepository;
import com.example.toget.domain.gift.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;

    /**
     * 위시리스트 아이템 생성. productId가 있으면 해당 상품의 위시리스트 등록 횟수를 1 증가시킨다.
     *
     * [설계 포인트]
     *  - 중복 검사는 (userId, productId, type) 기준이라 같은 상품을 GIVE/RECEIVE로 각각 담는 것은 허용된다.
     *  - productId가 null인 외부 링크 상품은 검증·카운트 대상이 아니다.
     */
    @Transactional
    public WishlistCreateResponse create(Long userId, WishlistCreateRequest request) {
        Long productId = request.productId();

        if (productId != null) {
            // soft delete된 상품까지 한 번에 걸러낸다
            productRepository.findByIdAndDeletedAtIsNull(productId)
                    .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));

            if (wishlistItemRepository.existsByUserIdAndProductIdAndType(userId, productId, request.type())) {
                throw new WishlistException(WishlistErrorCode.WISHLIST_DUPLICATE_PRODUCT);
            }
        }

        WishlistItem wishlistItem = WishlistConverter.toEntity(userId, request);
        WishlistItem savedItem = wishlistItemRepository.save(wishlistItem);

        if (productId != null) {
            productRepository.increaseWishlistCount(productId);
        }

        return WishlistConverter.toCreateResponse(savedItem);
    }

    public WishlistListResponse getWishlist(Long userId, int page, int size, WishlistSort sort) {
        return getWishlist(userId, null, page, size, sort);
    }

    public WishlistListResponse getWishlist(Long userId, WishlistType type, int page, int size, WishlistSort sort) {
        int safePage = Math.max(page, 0);
        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        Pageable pageable = PageRequest.of(safePage, safeSize);
        Slice<WishlistItem> slice;
        WishlistSort sortType = (sort != null) ? sort : WishlistSort.LATEST;

        if (type != null) {
            if (sortType == WishlistSort.OLDEST) {
                slice = wishlistItemRepository.findByUserIdAndTypeOrderByIdAsc(userId, type, pageable);
            } else {
                slice = wishlistItemRepository.findByUserIdAndTypeOrderByIdDesc(userId, type, pageable);
            }
        } else {
            if (sortType == WishlistSort.OLDEST) {
                slice = wishlistItemRepository.findByUserIdOrderByIdAsc(userId, pageable);
            } else {
                slice = wishlistItemRepository.findByUserIdOrderByIdDesc(userId, pageable);
            }
        }
        return WishlistConverter.toListResponse(slice);
    }

    /**
     * 위시리스트 아이템 수정.
     *
     * [설계 포인트]
     *  - productId는 수정 대상이 아니므로(WishlistUpdateRequest에 필드 없음) 카운트 재조정이 필요 없다.
     *    매핑을 바꾸려면 삭제 후 재등록해야 한다.
     *  - 다만 type은 수정 가능하고 유니크 제약(user_id, product_id, type)에 포함되므로,
     *    유형 변경이 같은 상품의 기존 항목과 충돌하지 않는지 검사한다.
     *    (예: 상품 5를 RECEIVE/GIVE로 각각 담아둔 상태에서 GIVE 항목을 RECEIVE로 바꾸는 경우)
     *  - 유형만 바뀌는 것은 해당 상품을 참조하는 행 수를 바꾸지 않으므로 카운트는 그대로 둔다.
     */
    @Transactional
    public WishlistUpdateResponse update(Long userId, Long wishlistItemId, WishlistUpdateRequest request) {
        WishlistItem item = wishlistItemRepository.findById(wishlistItemId)
                .orElseThrow(() -> new WishlistException(WishlistErrorCode.WISHLIST_NOT_FOUND));

        if (!item.getUserId().equals(userId)) {
            throw new WishlistException(WishlistErrorCode.WISHLIST_NOT_OWNER);
        }

        Long productId = item.getProductId();
        if (productId != null && item.getType() != request.type()
                && wishlistItemRepository.existsByUserIdAndProductIdAndType(userId, productId, request.type())) {
            throw new WishlistException(WishlistErrorCode.WISHLIST_DUPLICATE_PRODUCT);
        }

        item.update(request.name(), request.price(), request.purchaseUrl(), request.imageUrl(), request.type());
        return WishlistConverter.toUpdateResponse(item);
    }

    /** 위시리스트 아이템 삭제. 매핑된 상품이 있으면 위시리스트 등록 횟수를 1 감소시킨다. */
    @Transactional
    public void delete(Long userId, Long wishlistItemId) {
        WishlistItem item = wishlistItemRepository.findById(wishlistItemId)
                .orElseThrow(() -> new WishlistException(WishlistErrorCode.WISHLIST_NOT_FOUND));

        if (!item.getUserId().equals(userId)) {
            throw new WishlistException(WishlistErrorCode.WISHLIST_NOT_OWNER);
        }

        Long productId = item.getProductId();
        wishlistItemRepository.delete(item);

        if (productId != null) {
            productRepository.decreaseWishlistCount(productId);
        }
    }
}
