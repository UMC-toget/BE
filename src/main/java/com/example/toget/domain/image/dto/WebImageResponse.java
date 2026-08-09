package com.example.toget.domain.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "웹 이미지 검색 결과 항목")
public record WebImageResponse(

        @Schema(description = "실제 이미지 URL — 선택 시 그대로 프로필/위시/선물 이미지에 적용할 원본",
                example = "https://example.com/images/cake.jpg")
        String imageUrl,

        @Schema(description = "그리드에 보여줄 썸네일 URL — 구글이 제공하지 않으면 imageUrl과 동일한 값이 내려간다",
                example = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9Gc...")
        String thumbnailUrl,

        @Schema(description = "이 이미지가 게시된 원본 사이트 URL — '사이트 방문하기'에 사용. 구글이 주지 않으면 null",
                nullable = true,
                example = "https://example.com/blog/birthday-cake")
        String sourceUrl
) {
}
