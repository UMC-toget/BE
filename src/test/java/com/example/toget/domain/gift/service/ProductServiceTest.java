package com.example.toget.domain.gift.service;

import com.example.toget.domain.gift.dto.request.ProductCreateRequest;
import com.example.toget.domain.gift.dto.request.ProductUpdateRequest;
import com.example.toget.domain.gift.dto.response.ProductCreateResponse;
import com.example.toget.domain.gift.dto.response.ProductDetailResponse;
import com.example.toget.domain.gift.dto.response.ProductListResponse;
import com.example.toget.domain.gift.entity.Product;
import com.example.toget.domain.gift.exception.ProductException;
import com.example.toget.domain.gift.exception.code.ProductErrorCode;
import com.example.toget.domain.gift.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 테스트")
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("상품 등록 성공")
    public void createProduct_success() {
        // given
        ProductCreateRequest request = new ProductCreateRequest(
                "애플 워치 SE 2세대", 329000L, "스마트 워치", "https://example.com/image.jpg",
                "https://example.com/shop/1", "전자기기", "Apple"
        );

        Product savedProduct = Product.builder()
                .name(request.name())
                .price(request.price())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .purchaseUrl(request.purchaseUrl())
                .category(request.category())
                .brand(request.brand())
                .build();
        ReflectionTestUtils.setField(savedProduct, "id", 1L);

        given(productRepository.save(any(Product.class))).willReturn(savedProduct);

        // when
        ProductCreateResponse response = productService.createProduct(request);

        // then
        assertThat(response.productId()).isEqualTo(1L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("상품 상세 조회 성공")
    public void getProduct_success() {
        // given
        Long productId = 1L;
        Product product = Product.builder()
                .name("애플 워치 SE 2세대")
                .price(329000L)
                .description("스마트 워치")
                .imageUrl("https://example.com/image.jpg")
                .purchaseUrl("https://example.com/shop/1")
                .category("전자기기")
                .brand("Apple")
                .build();
        ReflectionTestUtils.setField(product, "id", productId);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // when
        ProductDetailResponse response = productService.getProduct(productId);

        // then
        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.name()).isEqualTo("애플 워치 SE 2세대");
        assertThat(response.price()).isEqualTo(329000L);
        assertThat(response.brand()).isEqualTo("Apple");
    }

    @Test
    @DisplayName("상품 상세 조회 실패 - 존재하지 않는 상품")
    public void getProduct_fail_notFound() {
        // given
        Long productId = 999L;
        given(productRepository.findById(productId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.getProduct(productId))
                .isInstanceOf(ProductException.class)
                .extracting(e -> ((ProductException) e).getCode())
                .isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("상품 목록 조회 성공")
    public void getProducts_success() {
        // given
        Product product = Product.builder()
                .name("애플 워치 SE 2세대")
                .price(329000L)
                .purchaseUrl("https://example.com/shop/1")
                .category("전자기기")
                .brand("Apple")
                .build();
        ReflectionTestUtils.setField(product, "id", 1L);

        Pageable pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"));
        Slice<Product> slice = new SliceImpl<>(List.of(product), pageable, false);

        given(productRepository.searchProducts(eq("전자기기"), eq("워치"), eq("Apple"), eq(100000L), eq(500000L), any(Pageable.class))).willReturn(slice);

        // when
        ProductListResponse response = productService.getProducts("전자기기", "워치", "Apple", 100000L, 500000L, 0, 10, "latest");

        // then
        assertThat(response.products()).hasSize(1);
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.pageSize()).isEqualTo(10);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.products().get(0).name()).isEqualTo("애플 워치 SE 2세대");
    }

    @Test
    @DisplayName("상품 수정 성공")
    public void updateProduct_success() {
        // given
        Long productId = 1L;
        Product product = Product.builder()
                .name("애플 워치 SE 2세대")
                .price(329000L)
                .purchaseUrl("https://example.com/shop/1")
                .category("전자기기")
                .brand("Apple")
                .build();
        ReflectionTestUtils.setField(product, "id", productId);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        ProductUpdateRequest request = new ProductUpdateRequest(
                "애플 워치 SE 2세대 (수정)", 359000L, "스마트 워치 신형",
                "https://example.com/image2.jpg", "https://example.com/shop/1",
                "전자기기", "Apple"
        );

        // when
        ProductDetailResponse response = productService.updateProduct(productId, request);

        // then
        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.name()).isEqualTo("애플 워치 SE 2세대 (수정)");
        assertThat(response.price()).isEqualTo(359000L);
    }

    @Test
    @DisplayName("상품 삭제 성공")
    public void deleteProduct_success() {
        // given
        Long productId = 1L;
        Product product = Product.builder()
                .name("애플 워치")
                .price(329000L)
                .purchaseUrl("https://example.com/shop/1")
                .build();
        ReflectionTestUtils.setField(product, "id", productId);

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // when
        productService.deleteProduct(productId);

        // then
        verify(productRepository).delete(product);
    }
}
