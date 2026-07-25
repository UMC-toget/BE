package com.example.toget.domain.gift.service;

import com.example.toget.domain.gift.dto.request.WishlistCreateRequest;
import com.example.toget.domain.gift.dto.request.WishlistUpdateRequest;
import com.example.toget.domain.gift.dto.response.WishlistCreateResponse;
import com.example.toget.domain.gift.dto.response.WishlistListResponse;
import com.example.toget.domain.gift.dto.response.WishlistUpdateResponse;
import com.example.toget.domain.gift.entity.WishlistItem;
import com.example.toget.domain.gift.exception.WishlistException;
import com.example.toget.domain.gift.exception.code.WishlistErrorCode;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("WishlistService 테스트")
public class WishlistServiceTest {

    @Mock
    private WishlistItemRepository wishlistItemRepository;

    @InjectMocks
    private WishlistService wishlistService;

    @Test
    @DisplayName("위시리스트 아이템 생성 성공")
    public void create_success() {
        // given
        Long userId = 1L;
        WishlistCreateRequest request = new WishlistCreateRequest(
                "맥북 프로 14", 2490000L, "https://apple.com", "https://image.com/macbook.png"
        );

        WishlistItem saved = WishlistItem.builder()
                .userId(userId)
                .name(request.name())
                .price(request.price())
                .purchaseUrl(request.purchaseUrl())
                .imageUrl(request.imageUrl())
                .build();
        ReflectionTestUtils.setField(saved, "id", 10L);

        given(wishlistItemRepository.save(any(WishlistItem.class))).willReturn(saved);

        // when
        WishlistCreateResponse response = wishlistService.create(userId, request);

        // then
        assertThat(response.wishlistItemId()).isEqualTo(10L);
        verify(wishlistItemRepository).save(any(WishlistItem.class));
    }

    @Test
    @DisplayName("위시리스트 목록 조회 성공")
    public void getWishlist_success() {
        // given
        Long userId = 1L;
        WishlistItem item = WishlistItem.builder()
                .userId(userId)
                .name("맥북 프로 14")
                .price(2490000L)
                .purchaseUrl("https://apple.com")
                .imageUrl("https://image.com/macbook.png")
                .build();
        ReflectionTestUtils.setField(item, "id", 10L);

        Pageable pageable = PageRequest.of(0, 10);
        Slice<WishlistItem> slice = new SliceImpl<>(List.of(item), pageable, false);

        given(wishlistItemRepository.findByUserIdOrderByIdDesc(userId, pageable)).willReturn(slice);

        // when
        WishlistListResponse response = wishlistService.getWishlist(userId, 0, 10, "latest");

        // then
        assertThat(response.wishlistItems()).hasSize(1);
        assertThat(response.wishlistItems().get(0).wishlistItemId()).isEqualTo(10L);
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.pageSize()).isEqualTo(10);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("위시리스트 수정 성공")
    public void update_success() {
        // given
        Long userId = 1L;
        Long wishlistItemId = 10L;
        WishlistItem item = WishlistItem.builder()
                .userId(userId)
                .name("맥북 프로 14")
                .price(2490000L)
                .purchaseUrl("https://apple.com")
                .imageUrl("https://image.com/macbook.png")
                .build();
        ReflectionTestUtils.setField(item, "id", wishlistItemId);

        given(wishlistItemRepository.findById(wishlistItemId)).willReturn(Optional.of(item));

        WishlistUpdateRequest request = new WishlistUpdateRequest(
                "맥북 프로 14 (M3)", 2390000L, "https://apple.com", "https://image.com/macbook-updated.png"
        );

        // when
        WishlistUpdateResponse response = wishlistService.update(userId, wishlistItemId, request);

        // then
        assertThat(response.wishlistItemId()).isEqualTo(wishlistItemId);
        assertThat(response.name()).isEqualTo("맥북 프로 14 (M3)");
        assertThat(response.price()).isEqualTo(2390000L);
        assertThat(response.imageUrl()).isEqualTo("https://image.com/macbook-updated.png");
    }

    @Test
    @DisplayName("위시리스트 수정 실패 - 존재하지 않는 아이템")
    public void update_fail_notFound() {
        // given
        Long userId = 1L;
        Long wishlistItemId = 999L;
        given(wishlistItemRepository.findById(wishlistItemId)).willReturn(Optional.empty());

        WishlistUpdateRequest request = new WishlistUpdateRequest(
                "맥북", 1000L, "https://apple.com", null
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
        WishlistItem item = WishlistItem.builder()
                .userId(otherUserId)
                .name("맥북 프로 14")
                .price(2490000L)
                .purchaseUrl("https://apple.com")
                .imageUrl(null)
                .build();
        ReflectionTestUtils.setField(item, "id", wishlistItemId);

        given(wishlistItemRepository.findById(wishlistItemId)).willReturn(Optional.of(item));

        WishlistUpdateRequest request = new WishlistUpdateRequest(
                "맥북", 1000L, "https://apple.com", null
        );

        // when & then
        assertThatThrownBy(() -> wishlistService.update(userId, wishlistItemId, request))
                .isInstanceOf(WishlistException.class)
                .extracting(e -> ((WishlistException) e).getCode())
                .isEqualTo(WishlistErrorCode.WISHLIST_NOT_OWNER);
    }

    @Test
    @DisplayName("위시리스트 삭제 성공")
    public void delete_success() {
        // given
        Long userId = 1L;
        Long wishlistItemId = 10L;
        WishlistItem item = WishlistItem.builder()
                .userId(userId)
                .name("맥북 프로 14")
                .price(2490000L)
                .purchaseUrl("https://apple.com")
                .imageUrl(null)
                .build();
        ReflectionTestUtils.setField(item, "id", wishlistItemId);

        given(wishlistItemRepository.findById(wishlistItemId)).willReturn(Optional.of(item));

        // when
        wishlistService.delete(userId, wishlistItemId);

        // then
        verify(wishlistItemRepository).delete(item);
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
    }

    @Test
    @DisplayName("위시리스트 삭제 실패 - 권한 없음")
    public void delete_fail_notOwner() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;
        Long wishlistItemId = 10L;
        WishlistItem item = WishlistItem.builder()
                .userId(otherUserId)
                .name("맥북 프로 14")
                .price(2490000L)
                .purchaseUrl("https://apple.com")
                .imageUrl(null)
                .build();
        ReflectionTestUtils.setField(item, "id", wishlistItemId);

        given(wishlistItemRepository.findById(wishlistItemId)).willReturn(Optional.of(item));

        // when & then
        assertThatThrownBy(() -> wishlistService.delete(userId, wishlistItemId))
                .isInstanceOf(WishlistException.class)
                .extracting(e -> ((WishlistException) e).getCode())
                .isEqualTo(WishlistErrorCode.WISHLIST_NOT_OWNER);

        verify(wishlistItemRepository, never()).delete(any());
    }
}
