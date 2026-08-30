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
            summary = "웹 사진 검색 (네이버 이미지 검색 프록시)",
            description = """
                    사진 업로드 바텀시트의 '웹 사진 검색' 탭에서 사용합니다.
                    Client ID/Secret 노출을 막기 위해 서버가 네이버 검색(이미지) API를 대신 호출합니다.

                    - imageUrl: 원본 이미지 URL. **이 값을 저장하지 말고 POST /images/imports에 넘겨
                      S3로 옮긴 뒤, 그 응답의 imageUrl을 저장하세요.**
                    - thumbnailUrl: 그리드 렌더링용
                    - sourceUrl: '사이트 방문하기'로 이동할 링크 (네이버 API 특성상 imageUrl과 동일)

                    한도 초과 시 429(IMAGE429_1)를 반환하므로, 프론트는 직접 업로드를 안내해주세요.

                    [참고] 당초 구글 이미지 검색으로 설계했으나 구글 Custom Search JSON API가
                    신규 고객에게 닫혀 있고(403) 2027-01-01 종료 예정이라 Pexels로 교체했다가,
                    실제 상품/브랜드 사진 검색이 잘 안 되는 문제로 네이버 이미지 검색으로 다시 교체했습니다.
                    네이버 결과는 Pexels와 달리 무료 라이선스 스톡 사진이 아니라 웹에 공개된 임의의
                    제3자 이미지이므로, 재배포 라이선스가 보장되지 않는다는 점을 알고 사용해야 합니다.
                    """
    )
    @GetMapping("/search")
    public ApiResponse<WebImageSearchResponse> searchWebImages(
            @Parameter(description = "검색어 (1~100자)", example = "생일 케이크", required = true)
            @RequestParam String query,

            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(required = false, defaultValue = "0") Integer page,

            @Parameter(description = "페이지당 개수 (1~80)", example = "20")
            @RequestParam(required = false, defaultValue = "20") Integer size
    ) {
        WebImageSearchResponse response = webImageSearchService.search(query, page, size);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, response);
    }
}
