package com.example.toget.domain.image.controller;

import com.example.toget.domain.image.dto.WebImageSearchResponse;
import com.example.toget.domain.image.service.WebImageSearchService;
import com.example.toget.global.apiPayload.ApiResponse;
import com.example.toget.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "이미지 API", description = "AWS S3 Presigned URL 발급 및 이미지 관련 API")
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class WebImageSearchController {

    private final WebImageSearchService webImageSearchService;

    @Operation(
            summary = "웹 사진 검색 (구글 이미지 검색 프록시)",
            description = """
                    사진 업로드 바텀시트의 '웹 사진 검색' 탭에서 사용합니다.
                    API 키 노출을 막기 위해 서버가 구글 Custom Search JSON API를 대신 호출합니다.

                    - imageUrl: 선택 시 그대로 프로필/위시/선물 이미지에 적용할 원본 URL
                    - thumbnailUrl: 그리드 렌더링용 (없으면 imageUrl과 동일)
                    - sourceUrl: '사이트 방문하기'로 이동할 원본 페이지 URL

                    한도 초과 시 429(IMAGE429_1)를 반환하므로, 프론트는 직접 업로드를 안내해주세요.
                    구글 제약상 최대 100건까지만 조회할 수 있습니다. (page * size + size <= 100)
                    """
    )
    @GetMapping("/search")
    public ApiResponse<WebImageSearchResponse> searchWebImages(
            @Parameter(description = "검색어 (1~100자)", example = "생일 케이크", required = true)
            @RequestParam String query,

            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(required = false, defaultValue = "0") Integer page,

            @Parameter(description = "페이지당 개수 (1~10)", example = "10")
            @RequestParam(required = false, defaultValue = "10") Integer size
    ) {
        WebImageSearchResponse response = webImageSearchService.search(query, page, size);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, response);
    }
}
