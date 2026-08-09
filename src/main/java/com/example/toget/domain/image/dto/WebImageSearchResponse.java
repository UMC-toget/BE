package com.example.toget.domain.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "웹 사진 검색 응답")
public record WebImageSearchResponse(

        @Schema(description = "검색된 이미지 목록. 결과가 없으면 빈 배열")
        List<WebImageResponse> images,

        @Schema(description = "다음 페이지 존재 여부 (무한 스크롤용)", example = "true")
        boolean hasNext
) {

    public static WebImageSearchResponse empty() {
        return new WebImageSearchResponse(List.of(), false);
    }
}
