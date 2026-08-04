package com.example.toget.domain.gift.service;

import com.example.toget.domain.gift.enums.WishlistSort;
import com.example.toget.domain.gift.enums.WishlistType;
import com.example.toget.domain.gift.dto.request.WishlistCreateRequest;
import com.example.toget.domain.gift.dto.request.WishlistUpdateRequest;
import com.example.toget.domain.gift.dto.response.WishlistCreateResponse;
import com.example.toget.domain.gift.dto.response.WishlistListResponse;
import com.example.toget.domain.gift.dto.response.WishlistUpdateResponse;
import com.example.toget.domain.gift.entity.Product;
import com.example.toget.domain.gift.entity.WishlistItem;
import com.example.toget.domain.gift.exception.ProductException;
import com.example.toget.domain.gift.exception.WishlistException;
import com.example.toget.domain.gift.exception.code.ProductErrorCode;
import com.example.toget.domain.gift.exception.code.WishlistErrorCode;
import com.example.toget.domain.gift.repository.ProductRepository;
import com.example.toget.domain.gift.repository.WishlistItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("WishlistService 테스트")
public class WishlistServiceTest {

    @Mock
    private WishlistItemRepository wishlistItemRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private WishlistService wishlistService;

    private WishlistItem wishlistItem(Long id, Long userId, Long productId, WishlistType type) {
        WishlistItem item = WishlistItem.builder()
                .userId(userId)
                .productId(productId)
                .name("맥북 프로 14")
                .price(2490000L)
                .purchaseUrl("https://apple.com")
                .imageUrl("https://image.com/macbook.png")
                .type(type)
                .build();
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    private Product product(Long id) {
        Product product = Product.builder()
                .name("맥북 프로 14")
                .price(2490000L)
                .purchaseUrl("https://apple.com")
                .build();
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    @Test
    @DisplayName("위시리스트 아이템 생성 성공 - 외부 링크 상품(productId 없음)")
    public void create_success() {
        // given
        Long userId = 1L;
        WishlistCreateRequest request = new WishlistCreateRequest(
                null, "맥북 프로 14", 2490000L, "https://apple.com", "https://image.com/macbook.png", WishlistType.RECEIVE
        );

        given(wishlistItemRepository.save(any(WishlistItem.class)))
                .willReturn(wishlistItem(10L, userId, null, WishlistType.RECEIVE));

        // when
        WishlistCreateResponse response = wishlistService.create(userId, request);

        // then
        assertThat(response.wishlistItemId()).isEqualTo(10L);
        assertThat(response.productId()).isNull();
        verify(wishlistItemRepository).save(any(WishlistItem.class));
        // productId가 없으면 카운트 증가 대상이 아니다
        verify(productRepository, never()).increaseWishlistCount(anyLong());
    }

    @Test
    @DisplayName("위시리스트 아이템 생성 성공 - 자체 상품 매핑 시 위시리스트 등록 횟수 +1")
    public void create_withProduct_increasesWishlistCount() {
        // given
        Long userId = 1L;
        Long productId = 5L;
        WishlistCreateRequest request = new WishlistCreateRequest(
                productId, "맥북 프로 14", 2490000L, "https://apple.com", "https://image.com/macbook.png", WishlistType.RECEIVE
        );

        given(productRepository.findByIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(product(productId)));
        given(wishlistItemRepository.existsByUserIdAndProductIdAndType(userId, productId, WishlistType.RECEIVE))
                .willReturn(false);
        given(wishlistItemRepository.save(any(WishlistItem.class)))
                .willReturn(wishlistItem(10L, userId, productId, WishlistType.RECEIVE));

        // when
        WishlistCreateResponse response = wishlistService.create(userId, request);

        // then
        assertThat(response.wishlistItemId()).isEqualTo(10L);
        assertThat(response.productId()).isEqualTo(productId);
        verify(productRepository).increaseWishlistCount(productId);
    }

    @Test
    @DisplayName("위시리스트 아이템 생성 성공 - 같은 상품을 다른 유형으로 등록 (GIVE/RECEIVE 병행 허용)")
    public void create_sameProductDifferentType_success() {
        // given
        Long userId = 1L;
        Long productId = 5L;
        // RECEIVE로는 이미 담겨 있으나 GIVE로는 등록된 적이 없는 상태
        WishlistCreateRequest request = new WishlistCreateRequest(
                productId, "맥북 프로 14", 2490000L, "https://apple.com", "https://image.com/macbook.png", WishlistType.GIVE
        );

        given(productRepository.findByIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(product(productId)));
        given(wishlistItemRepository.existsByUserIdAndProductIdAndType(userId, productId, WishlistType.GIVE))
                .willReturn(false);
        given(wishlistItemRepository.save(any(WishlistItem.class)))
                .willReturn(wishlistItem(11L, userId, productId, WishlistType.GIVE));

        // when
        WishlistCreateResponse response = wishlistService.create(userId, request);

        // then
        assertThat(response.wishlistItemId()).isEqualTo(11L);
        // 한 유저가 GIVE/RECEIVE 둘 다 담으면 카운트는 총 +2가 된다 (행 기준 집계)
        verify(productRepository).increaseWishlistCount(productId);
    }

    @Test
    @DisplayName("위시리스트 아이템 생성 실패 - 존재하지 않거나 삭제된 상품")
    public void create_fail_productNotFound() {
        // given
        Long userId = 1L;
        Long productId = 999L;
        WishlistCreateRequest request = new WishlistCreateRequest(
                productId, "맥북 프로 14", 2490000L, "https://apple.com", null, WishlistType.RECEIVE
        );

        given(productRepository.findByIdAndDeletedAtIsNull(productId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> wishlistService.create(userId, request))
                .isInstanceOf(ProductException.class)
                .extracting(e -> ((ProductException) e).getCode())
                .isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND);

        verify(wishlistItemRepository, never()).save(any());
        verify(productRepository, never()).increaseWishlistCount(anyLong());
    }

    @Test
    @DisplayName("위시리스트 아이템 생성 실패 - 동일 유형으로 중복 등록")
    public void create_fail_duplicateProduct() {
        // given
        Long userId = 1L;
        Long productId = 5L;
        WishlistCreateRequest request = new WishlistCreateRequest(
                productId, "맥북 프로 14", 2490000L, "https://apple.com", null, WishlistType.RECEIVE
        );

        given(productRepository.findByIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(product(productId)));
        given(wishlistItemRepository.existsByUserIdAndProductIdAndType(userId, productId, WishlistType.RECEIVE))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> wishlistService.create(userId, request))
                .isInstanceOf(WishlistException.class)
                .extracting(e -> ((WishlistException) e).getCode())
                .isEqualTo(WishlistErrorCode.WISHLIST_DUPLICATE_PRODUCT);

        verify(wishlistItemRepository, never()).save(any());
        verify(productRepository, never()).increaseWishlistCount(anyLong());
    }

    @Test
    @DisplayName("위시리스트 목록 조회 성공")
    public void getWishlist_success() {
        // given
        Long userId = 1L;
        WishlistItem item = wishlistItem(10L, userId, 5L, WishlistType.RECEIVE);

        Pageable pageable = PageRequest.of(0, 10);
        Slice<WishlistItem> slice = new SliceImpl<>(List.of(item), pageable, false);

        given(wishlistItemRepository.findByUserIdOrderByIdDesc(userId, pageable)).willReturn(slice);

        // when
        WishlistListResponse response = wishlistService.getWishlist(userId, 0, 10, WishlistSort.LATEST);

        // then
        assertThat(response.wishlistItems()).hasSize(1);
        assertThat(response.wishlistItems().get(0).wishlistItemId()).isEqualTo(10L);
        assertThat(response.wishlistItems().get(0).productId()).isEqualTo(5L);
        assertThat(response.wishlistItems().get(0).type()).isEqualTo(WishlistType.RECEIVE);
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.pageSize()).isEqualTo(10);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("위시리스트 목록 조회 성공 - 유형 필터링 (GIVE)")
    public void getWishlist_withTypeFilter_success() {
        // given
        Long userId = 1L;
        WishlistItem item = wishlistItem(11L, userId, null, WishlistType.GIVE);

        Pageable pageable = PageRequest.of(0, 10);
        Slice<WishlistItem> slice = new SliceImpl<>(List.of(item), pageable, false);

        given(wishlistItemRepository.findByUserIdAndTypeOrderByIdDesc(userId, WishlistType.GIVE, pageable)).willReturn(slice);

        // when
        WishlistListResponse response = wishlistService.getWishlist(userId, WishlistType.GIVE, 0, 10, WishlistSort.LATEST);

        // then
        assertThat(response.wishlistItems()).hasSize(1);
        assertThat(response.wishlistItems().get(0).wishlistItemId()).isEqualTo(11L);
        assertThat(response.wishlistItems().get(0).type()).isEqualTo(WishlistType.GIVE);
    }

    @Test
    @DisplayName("위시리스트 수정 성공 - 상품 매핑은 변경되지 않는다")
    public void update_success() {
        // given
        Long userId = 1L;
        Long wishlistItemId = 10L;
        Long productId = 5L;
        WishlistItem item = wishlistItem(wishlistItemId, userId, productId, WishlistType.RECEIVE);

        given(wishlistItemRepository.findById(wishlistItemId)).willReturn(Optional.of(item));

        WishlistUpdateRequest request = new WishlistUpdateRequest(
                "맥북 프로 14 (M3)", 2390000L, "https://apple.com", "https://image.com/macbook-updated.png", WishlistType.GIVE
        );

        // when
        WishlistUpdateResponse response = wishlistService.update(userId, wishlistItemId, request);

        // then
        assertThat(response.wishlistItemId()).isEqualTo(wishlistItemId);
        assertThat(response.name()).isEqualTo("맥북 프로 14 (M3)");
        assertThat(response.price()).isEqualTo(2390000L);
        assertThat(response.imageUrl()).isEqualTo("https://image.com/macbook-updated.png");
        assertThat(response.type()).isEqualTo(WishlistType.GIVE);
        // productId는 수정 대상이 아니므로 카운트 재조정이 발생하지 않는다
        assertThat(item.getProductId()).isEqualTo(productId);
        verify(productRepository, never()).increaseWishlistCount(anyLong());
        verify(productRepository, never()).decreaseWishlistCount(anyLong());
    }

    @Test
    @DisplayName("위시리스트 수정 실패 - 같은 상품의 기존 항목과 유형이 충돌")
    public void update_fail_duplicateTypeAfterChange() {
        // given: 상품 5를 GIVE로 담아둔 항목을 RECEIVE로 바꾸려는데, RECEIVE 항목이 이미 존재하는 상황
        Long userId = 1L;
        Long wishlistItemId = 11L;
        Long productId = 5L;
        WishlistItem item = wishlistItem(wishlistItemId, userId, productId, WishlistType.GIVE);

        given(wishlistItemRepository.findById(wishlistItemId)).willReturn(Optional.of(item));
        given(wishlistItemRepository.existsByUserIdAndProductIdAndType(userId, productId, WishlistType.RECEIVE))
                .willReturn(true);

        WishlistUpdateRequest request = new WishlistUpdateRequest(
                "맥북 프로 14", 2490000L, "https://apple.com", null, WishlistType.RECEIVE
        );

        // when & then
        assertThatThrownBy(() -> wishlistService.update(userId, wishlistItemId, request))
                .isInstanceOf(WishlistException.class)
                .extracting(e -> ((WishlistException) e).getCode())
                .isEqualTo(WishlistErrorCode.WISHLIST_DUPLICATE_PRODUCT);

        // 유형은 변경되지 않아야 한다
        assertThat(item.getType()).isEqualTo(WishlistType.GIVE);
    }

    @Test
    @DisplayName("위시리스트 수정 성공 - 유형 변경 시 충돌이 없으면 카운트는 그대로")
    public void update_typeChanged_countUnchanged() {
        // given
        Long userId = 1L;
        Long wishlistItemId = 10L;
        Long productId = 5L;
        WishlistItem item = wishlistItem(wishlistItemId, userId, productId, WishlistType.RECEIVE);

        given(wishlistItemRepository.findById(wishlistItemId)).willReturn(Optional.of(item));
        given(wishlistItemRepository.existsByUserIdAndProductIdAndType(userId, productId, WishlistType.GIVE))
                .willReturn(false);

        WishlistUpdateRequest request = new WishlistUpdateRequest(
                "맥북 프로 14", 2490000L, "https://apple.com", null, WishlistType.GIVE
        );

        // when
        WishlistUpdateResponse response = wishlistService.update(userId, wishlistItemId, request);

        // then
        assertThat(response.type()).isEqualTo(WishlistType.GIVE);
        // 상품을 참조하는 행 수는 그대로이므로 카운트도 변하지 않는다
        verify(productRepository, never()).increaseWishlistCount(anyLong());
        verify(productRepository, never()).decreaseWishlistCount(anyLong());
    }

    @Test
    @DisplayName("위시리스트 수정 실패 - 존재하지 않는 아이템")
    public void update_fail_notFound() {
        // given
        Long userId = 1L;
        Long wishlistItemId = 999L;
        given(wishlistItemRepository.findById(wishlistItemId)).willReturn(Optional.empty());

        WishlistUpdateRequest request = new WishlistUpdateRequest(
                "맥북", 1000L, "https://apple.com", null, WishlistType.RECEIVE
        );

        // when & then
        assertThatThrownBy(() -> wishlistService.update(userId, wishlistItemId, request))
                .isInstanceOf(WishlistException.class)
                .extracting(e -> ((WishlistException) e).getCode())
                .isEqualTo(WishlistErrorCode.WISHLIST_NOT_FOUND);
    }

    @Test
    @DisplayName("위시리스트 수정 실패 - 권한 없음")
    public void update_fail_notOwner() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;
        Long wishlistItemId = 10L;
        WishlistItem item = wishlistItem(wishlistItemId, otherUserId, null, WishlistType.RECEIVE);

        given(wishlistItemRepository.findById(wishlistItemId)).willReturn(Optional.of(item));

        WishlistUpdateRequest request = new WishlistUpdateRequest(
                "맥북", 1000L, "https://apple.com", null, WishlistType.RECEIVE
        );

        // when & then
        assertThatThrownBy(() -> wishlistService.update(userId, wishlistItemId, request))
                .isInstanceOf(WishlistException.class)
                .extracting(e -> ((WishlistException) e).getCode())
                .isEqualTo(WishlistErrorCode.WISHLIST_NOT_OWNER);
    }

    @Test
    @DisplayName("위시리스트 삭제 성공 - 자체 상품 매핑 시 위시리스트 등록 횟수 -1")
    public void delete_withProduct_decreasesWishlistCount() {
        // given
        Long userId = 1L;
        Long wishlistItemId = 10L;
        Long productId = 5L;
        WishlistItem item = wishlistItem(wishlistItemId, userId, productId, WishlistType.RECEIVE);

        given(wishlistItemRepository.findById(wishlistItemId)).willReturn(Optional.of(item));

        // when
        wishlistService.delete(userId, wishlistItemId);

        // then
        verify(wishlistItemRepository).delete(item);
        verify(productRepository).decreaseWishlistCount(productId);
    }

    @Test
    @DisplayName("위시리스트 삭제 성공 - 외부 링크 상품은 카운트가 변하지 않는다")
    public void delete_withoutProduct_success() {
        // given
        Long userId = 1L;
        Long wishlistItemId = 10L;
        WishlistItem item = wishlistItem(wishlistItemId, userId, null, WishlistType.RECEIVE);

        given(wishlistItemRepository.findById(wishlistItemId)).willReturn(Optional.of(item));

        // when
        wishlistService.delete(userId, wishlistItemId);

        // then
        verify(wishlistItemRepository).delete(item);
        verify(productRepository, never()).decreaseWishlistCount(anyLong());
    }

    @Test
    @DisplayName("위시리스트 삭제 실패 - 존재하지 않는 아이템")
    public void delete_fail_notFound() {
        // given
        Long userId = 1L;
        Long wishlistItemId = 999L;
        given(wishlistItemRepository.findById(wishlistItemId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> wishlistService.delete(userId, wishlistItemId))
                .isInstanceOf(WishlistException.class)
                .extracting(e -> ((WishlistException) e).getCode())
                .isEqualTo(WishlistErrorCode.WISHLIST_NOT_FOUND);

        verify(wishlistItemRepository, never()).delete(any());
        verify(productRepository, never()).decreaseWishlistCount(anyLong());
    }

    @Test
    @DisplayName("위시리스트 삭제 실패 - 권한 없음")
    public void delete_fail_notOwner() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;
        Long wishlistItemId = 10L;
        WishlistItem item = wishlistItem(wishlistItemId, otherUserId, 5L, WishlistType.RECEIVE);

        given(wishlistItemRepository.findById(wishlistItemId)).willReturn(Optional.of(item));

        // when & then
        assertThatThrownBy(() -> wishlistService.delete(userId, wishlistItemId))
                .isInstanceOf(WishlistException.class)
                .extracting(e -> ((WishlistException) e).getCode())
                .isEqualTo(WishlistErrorCode.WISHLIST_NOT_OWNER);

        verify(wishlistItemRepository, never()).delete(any());
        verify(productRepository, never()).decreaseWishlistCount(anyLong());
    }
}
