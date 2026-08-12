package com.example.toget.domain.gift.controller;

import com.example.toget.domain.gift.dto.request.ProductCreateRequest;
import com.example.toget.domain.gift.dto.request.ProductUpdateRequest;
import com.example.toget.domain.gift.dto.response.ProductCreateResponse;
import com.example.toget.domain.gift.dto.response.ProductDetailResponse;
import com.example.toget.domain.gift.enums.CategoryType;
import com.example.toget.domain.gift.service.ProductService;
import com.example.toget.domain.user.service.ActiveUserReader;
import com.example.toget.global.config.AdminProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.autoconfigure.web.DataWebAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductController.class, excludeAutoConfiguration = DataWebAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ProductController 테스트")
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private AdminProperties adminProperties;

    @MockitoBean
    private ActiveUserReader activeUserReader;

    @Test
    @DisplayName("[POST /api/v1/products] 상품 등록 성공 - 다중 카테고리 지정")
    public void createProduct_success() throws Exception {
        // given
        ProductCreateRequest request = new ProductCreateRequest(
                "기프트 카드", 30000L, "선물용 카드", "https://example.com/image.jpg",
                "https://example.com/shop/1", List.of(CategoryType.BIRTHDAY, CategoryType.GRADUATION), "Toget"
        );

        given(productService.createProduct(any(ProductCreateRequest.class)))
                .willReturn(new ProductCreateResponse(1L));

        // when & then
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.productId").value(1L));
    }

    @Test
    @DisplayName("[POST /api/v1/products] 상품 등록 실패 - 카테고리 미지정 (빈 리스트)")
    public void createProduct_fail_emptyCategory() throws Exception {
        // given
        ProductCreateRequest request = new ProductCreateRequest(
                "기프트 카드", 30000L, "선물용 카드", "https://example.com/image.jpg",
                "https://example.com/shop/1", Collections.emptyList(), "Toget"
        );

        // when & then
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[PUT /api/v1/products/{productId}] 상품 수정 성공 - 다중 카테고리 수정")
    public void updateProduct_success() throws Exception {
        // given
        Long productId = 1L;
        ProductUpdateRequest request = new ProductUpdateRequest(
                "기프트 카드 (수정)", 35000L, "선물용 카드 (수정)", "https://example.com/image2.jpg",
                "https://example.com/shop/1", List.of(CategoryType.HOUSEWARMING), "Toget"
        );

        ProductDetailResponse response = new ProductDetailResponse(
                productId, request.name(), request.price(), request.description(),
                request.imageUrl(), request.purchaseUrl(), request.categoryTypes(),
                request.brand(), 0L, LocalDateTime.now(), LocalDateTime.now()
        );

        given(productService.updateProduct(eq(productId), any(ProductUpdateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(put("/api/v1/products/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.productId").value(productId))
                .andExpect(jsonPath("$.result.categoryTypes[0]").value("HOUSEWARMING"));
    }
}
