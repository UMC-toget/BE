package com.example.toget.domain.image.service;

import com.example.toget.domain.image.dto.WebImageResponse;
import com.example.toget.domain.image.dto.WebImageSearchResponse;
import com.example.toget.domain.image.exception.ImageException;
import com.example.toget.domain.image.exception.code.ImageErrorCode;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * WebImageSearchService 단위 테스트 — Pexels 전환 버전.
 *
 * <p>MockRestServiceServer로 Pexels API를 흉내 내어, 외부 키 없이 계약을 검증한다.
 *
 * <p>핵심 계약:
 * <ul>
 *   <li>API 키는 <b>URL이 아니라 Authorization 헤더</b>로만 나간다 (로그 유출 방지)</li>
 *   <li>프론트 계약인 0-based page를 Pexels의 1-based page로 변환한다</li>
 *   <li>키 미설정이면 외부 호출조차 하지 않고 503으로 거부 (fail-fast)</li>
 *   <li>쿼터 소진(429)과 그 외 실패(502)를 섞지 않는다 — 프론트 안내 문구가 갈리기 때문</li>
 *   <li>결과가 없거나 필드가 빠져도 500이 아니라 정상 응답으로 처리한다</li>
 * </ul>
 */
class WebImageSearchServiceTest {

    private static final String SEARCH_URI = "https://api.pexels.com/v1/search";
    private static final String API_KEY = "test-pexels-key";
    private static final String LOCALE = "ko-KR";

    private MockRestServiceServer server;
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
    }

    private WebImageSearchService service() {
        return service(API_KEY);
    }

    private WebImageSearchService service(String apiKey) {
        return new WebImageSearchService(restClient, SEARCH_URI, apiKey, LOCALE);
    }

    /** 기대 URL — 키가 들어가지 않는다는 점이 이 문자열의 핵심이다 */
    private static String url(String encodedQuery, int page, int perPage) {
        return SEARCH_URI + "?query=" + encodedQuery + "&page=" + page
                + "&per_page=" + perPage + "&locale=" + LOCALE;
    }

    private void expectSearch(String encodedQuery, int page, int perPage, String responseJson) {
        server.expect(requestTo(url(encodedQuery, page, perPage)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", API_KEY))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
    }

    private static void assertRejectedWith(ImageErrorCode expected, ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(ImageException.class)
                .satisfies(e -> assertThat(((ImageException) e).getCode()).isEqualTo(expected));
    }

    private static String photo(String large2x, String medium) {
        return """
                {
                  "url": "https://www.pexels.com/ko-kr/photo/2014422/",
                  "photographer": "Joey Farina",
                  "photographer_url": "https://www.pexels.com/ko-kr/@joey",
                  "alt": "갈색 바위",
                  "src": {
                    "original": "https://img/original.jpg",
                    "large2x": "%s",
                    "medium": "%s"
                  }
                }
                """.formatted(large2x, medium);
    }

    // ------------------------------------------------------------ 정상 경로

    @Test
    @DisplayName("검색 결과를 imageUrl/thumbnailUrl/sourceUrl/작가 정보로 매핑한다")
    void mapsPexelsPhotoToResponse() {
        // next_page URL이 /v1/v1/로 중복되어 오는 실제 Pexels 응답을 그대로 재현한다.
        // 우리는 존재 여부만 보므로 이 버그에 영향받지 않아야 한다.
        expectSearch("perfume", 1, 20, """
                {
                  "photos": [%s],
                  "next_page": "https://api.pexels.com/v1/v1/search?page=2"
                }
                """.formatted(photo("https://img/large2x.jpg", "https://img/medium.jpg")));

        WebImageSearchResponse response = service().search("perfume", 0, 20);

        assertThat(response.hasNext()).isTrue();
        assertThat(response.images()).hasSize(1);
        WebImageResponse image = response.images().get(0);
        // original이 함께 와도 large2x를 우선한다 (원본은 5000px/수 MB라 과하다)
        assertThat(image.imageUrl()).isEqualTo("https://img/large2x.jpg");
        assertThat(image.thumbnailUrl()).isEqualTo("https://img/medium.jpg");
        assertThat(image.sourceUrl()).isEqualTo("https://www.pexels.com/ko-kr/photo/2014422/");
        // 라이선스상 표기가 필요한 값이라 응답에서 누락되면 안 된다
        assertThat(image.photographer()).isEqualTo("Joey Farina");
        assertThat(image.photographerUrl()).isEqualTo("https://www.pexels.com/ko-kr/@joey");
        assertThat(image.alt()).isEqualTo("갈색 바위");
        server.verify();
    }

    @Test
    @DisplayName("large2x가 없으면 large → original 순으로 폴백한다")
    void fallsBackWhenLarge2xMissing() {
        expectSearch("perfume", 1, 20, """
                {"photos":[{"src":{"large":"https://img/large.jpg","original":"https://img/o.jpg"}}]}
                """);

        WebImageSearchResponse response = service().search("perfume", 0, 20);

        assertThat(response.images().get(0).imageUrl()).isEqualTo("https://img/large.jpg");
    }

    @Test
    @DisplayName("API 키는 URL이 아니라 Authorization 헤더로 전달된다")
    void sendsApiKeyOnlyInHeader() {
        // url()에 키가 없으므로, 키가 쿼리로 새면 requestTo 매칭이 실패한다
        expectSearch("perfume", 1, 20, "{\"photos\":[]}");

        service().search("perfume", 0, 20);

        server.verify();
    }

    @Test
    @DisplayName("0부터 시작하는 page를 Pexels의 1-based page로 변환한다")
    void convertsZeroBasedPageToOneBased() {
        expectSearch("perfume", 4, 15, "{\"photos\":[]}");

        service().search("perfume", 3, 15);

        server.verify();
    }

    @Test
    @DisplayName("한글 검색어는 URL 인코딩되어 전달된다")
    void encodesKoreanQuery() {
        expectSearch("%ED%96%A5%EC%88%98", 1, 20, "{\"photos\":[]}");

        service().search("향수", 0, 20);

        server.verify();
    }

    @Test
    @DisplayName("size 미지정 시 기본값 20으로 조회한다")
    void usesDefaultSize() {
        expectSearch("perfume", 1, 20, "{\"photos\":[]}");

        service().search("perfume", null, null);

        server.verify();
    }

    // ------------------------------------------------------------ 응답 방어

    @Test
    @DisplayName("medium 썸네일이 없으면 적용 이미지를 재사용한다")
    void fallsBackToImageUrlWhenThumbnailMissing() {
        expectSearch("perfume", 1, 20, """
                {"photos":[{"src":{"large2x":"https://img/large2x.jpg"}}]}
                """);

        WebImageSearchResponse response = service().search("perfume", 0, 20);

        assertThat(response.images().get(0).thumbnailUrl()).isEqualTo("https://img/large2x.jpg");
    }

    @Test
    @DisplayName("쓸 수 있는 이미지 URL이 하나도 없으면 결과에서 제외한다")
    void skipsPhotoWithoutUsableImageUrl() {
        expectSearch("perfume", 1, 20, """
                {"photos":[{"src":{"medium":"https://img/medium.jpg"}}]}
                """);

        WebImageSearchResponse response = service().search("perfume", 0, 20);

        assertThat(response.images()).isEmpty();
    }

    @Test
    @DisplayName("photos가 없거나 비어 있어도 빈 배열로 정상 응답한다")
    void returnsEmptyListWhenNoPhotos() {
        expectSearch("perfume", 1, 20, "{}");

        WebImageSearchResponse response = service().search("perfume", 0, 20);

        assertThat(response.images()).isEmpty();
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("next_page가 없으면 hasNext는 false다")
    void hasNextIsFalseWithoutNextPage() {
        expectSearch("perfume", 1, 20, """
                {"photos":[%s]}
                """.formatted(photo("https://img/o.jpg", "https://img/m.jpg")));

        WebImageSearchResponse response = service().search("perfume", 0, 20);

        assertThat(response.hasNext()).isFalse();
    }

    // ------------------------------------------------------------ 실패 분류

    @Test
    @DisplayName("429는 쿼터 소진(IMAGE429_1)으로 분류한다")
    void classifiesTooManyRequestsAsQuotaExceeded() {
        server.expect(requestTo(url("perfume", 1, 20)))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertRejectedWith(ImageErrorCode.SEARCH_QUOTA_EXCEEDED,
                () -> service().search("perfume", 0, 20));
    }

    @Test
    @DisplayName("401(잘못된 키)은 사용자 입력 탓이 아니므로 502로 감춘다")
    void classifiesUnauthorizedAsProviderUnavailable() {
        server.expect(requestTo(url("perfume", 1, 20)))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertRejectedWith(ImageErrorCode.SEARCH_PROVIDER_UNAVAILABLE,
                () -> service().search("perfume", 0, 20));
    }

    @Test
    @DisplayName("Pexels 5xx는 502로 변환한다")
    void classifiesServerErrorAsProviderUnavailable() {
        server.expect(requestTo(url("perfume", 1, 20)))
                .andRespond(withServerError());

        assertRejectedWith(ImageErrorCode.SEARCH_PROVIDER_UNAVAILABLE,
                () -> service().search("perfume", 0, 20));
    }

    @Test
    @DisplayName("타임아웃도 502로 변환한다 (429와 섞이지 않는다)")
    void classifiesTimeoutAsProviderUnavailable() {
        server.expect(requestTo(url("perfume", 1, 20)))
                .andRespond(withException(new SocketTimeoutException("timeout")));

        assertRejectedWith(ImageErrorCode.SEARCH_PROVIDER_UNAVAILABLE,
                () -> service().search("perfume", 0, 20));
    }

    // ------------------------------------------------------------ 요청 검증

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("검색어가 비어 있으면 외부 호출 없이 400으로 거부한다")
    void rejectsBlankQuery(String query) {
        assertRejectedWith(ImageErrorCode.INVALID_SEARCH_QUERY,
                () -> service().search(query, 0, 20));
        server.verify(); // 호출이 나갔다면 기대하지 않은 요청으로 실패한다
    }

    @Test
    @DisplayName("검색어가 100자를 넘으면 400으로 거부한다")
    void rejectsTooLongQuery() {
        assertRejectedWith(ImageErrorCode.INVALID_SEARCH_QUERY,
                () -> service().search("a".repeat(101), 0, 20));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 81})
    @DisplayName("size가 1~80 범위를 벗어나면 400으로 거부한다")
    void rejectsInvalidSize(int size) {
        assertRejectedWith(ImageErrorCode.INVALID_PAGE_SIZE,
                () -> service().search("perfume", 0, size));
    }

    @Test
    @DisplayName("음수 page는 0으로 보정한다 (첫 페이지 조회)")
    void normalizesNegativePage() {
        expectSearch("perfume", 1, 20, "{\"photos\":[]}");

        service().search("perfume", -5, 20);

        server.verify();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("API 키가 없으면 외부 호출조차 하지 않고 503으로 거부한다 (fail-fast)")
    void rejectsWhenApiKeyMissing(String apiKey) {
        String key = apiKey == null ? "" : apiKey; // @Value 기본값이 빈 문자열이라 null은 오지 않는다
        assertRejectedWith(ImageErrorCode.SEARCH_NOT_CONFIGURED,
                () -> service(key).search("perfume", 0, 20));
        server.verify();
    }
}
