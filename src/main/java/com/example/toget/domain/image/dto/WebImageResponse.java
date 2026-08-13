package com.example.toget.domain.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "웹 이미지 검색 결과 항목 (네이버 이미지 검색)")
public record WebImageResponse(

        @Schema(description = """
                적용할 이미지 URL. **이 값을 POST /images/imports에 넘겨 S3로 옮긴 뒤,
                그 응답의 imageUrl을 저장하세요.**
                """,
                example = "https://blogfiles.pstatic.net/example.jpg")
        String imageUrl,

        @Schema(description = "그리드에 보여줄 썸네일 URL. 원본을 그대로 걸면 목록이 무거워진다",
                example = "https://search.pstatic.net/example_thumb.jpg")
        String thumbnailUrl,

        @Schema(description = """
                이미지 원본 링크 — '사이트 방문하기'에 사용.
                네이버 이미지 검색은 원본이 게시된 페이지 링크를 별도로 제공하지 않아 imageUrl과 동일하다.
                """,
                nullable = true,
                example = "https://blogfiles.pstatic.net/example.jpg")
        String sourceUrl,

        @Schema(description = "이미지 설명(네이버 검색어 강조 태그 제거됨) — 접근성용",
                nullable = true,
                example = "생일 케이크")
        String alt
) {
}
