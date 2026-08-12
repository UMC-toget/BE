package com.example.toget.domain.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "웹 이미지 검색 결과 항목 (Pexels)")
public record WebImageResponse(

        @Schema(description = """
                적용할 이미지 URL(940×650 @2x). **이 값을 POST /images/imports에 넘겨 S3로 옮긴 뒤,
                그 응답의 imageUrl을 저장하세요.**
                Pexels 원본(original)은 5000px/수 MB급이라 프로필·선물 이미지로는 과해서
                large2x를 기본으로 내려줍니다.
                """,
                example = "https://images.pexels.com/photos/2014422/pexels-photo-2014422.jpeg?auto=compress&dpr=2&h=650&w=940")
        String imageUrl,

        @Schema(description = "그리드에 보여줄 썸네일 URL(높이 350px). 원본을 그대로 걸면 목록이 무거워진다",
                example = "https://images.pexels.com/photos/2014422/pexels-photo-2014422.jpeg?auto=compress&h=350")
        String thumbnailUrl,

        @Schema(description = "Pexels 사진 페이지 URL — '사이트 방문하기'에 사용",
                nullable = true,
                example = "https://www.pexels.com/photo/brown-rocks-during-golden-hour-2014422/")
        String sourceUrl,

        @Schema(description = """
                사진작가 이름. **Pexels 라이선스상 표기가 요구되므로 화면에 노출해야 한다.**
                예: "Photo by {photographer} on Pexels" (photographerUrl로 링크)
                """,
                nullable = true,
                example = "Joey Farina")
        String photographer,

        @Schema(description = "사진작가의 Pexels 프로필 URL — 작가명에 걸 링크",
                nullable = true,
                example = "https://www.pexels.com/@joey")
        String photographerUrl,

        @Schema(description = "이미지 대체 텍스트(alt) — 접근성용",
                nullable = true,
                example = "Brown Rocks During Golden Hour")
        String alt
) {
}
