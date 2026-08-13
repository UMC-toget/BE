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
 * WebImageSearchService 단위 테스트 — 네이버 이미지 검색 전환 버전.
 *
 * <p>MockRestServiceServer로 네이버 검색 API를 흉내 내어, 외부 키 없이 계약을 검증한다.
 *
 * <p>핵심 계약:
 * <ul>
 *   <li>Client ID/Secret은 <b>URL이 아니라 전용 헤더</b>로만 나간다 (로그 유출 방지)</li>
 *   <li>프론트 계약인 0-based page를 네이버의 1-based start로 변환한다</li>
 *   <li>start+display가 1000을 넘는 조회는 외부 호출 없이 빈 결과로 처리한다</li>
 *   <li>키 미설정이면 외부 호출조차 하지 않고 503으로 거부 (fail-fast)</li>
 *   <li>한도 소진(429)과 그 외 실패(502)를 섞지 않는다 — 프론트 안내 문구가 갈리기 때문</li>
 *   <li>결과가 없거나 필드가 빠져도 500이 아니라 정상 응답으로 처리한다</li>
 * </ul>
 */
class WebImageSearchServiceTest {

    private static final String SEARCH_URI = "https://naverapihub.apigw.ntruss.com/search/v1/image";
    private static final String CLIENT_ID = "test-client-id";
    private static final String CLIENT_SECRET = "test-client-secret";
    private static final String FILTER = "large";

    private MockRestServiceServer server;
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
    }

    private WebImageSearchService service() {
        return service(CLIENT_ID, CLIENT_SECRET);
    }

    private WebImageSearchService service(String clientId, String clientSecret) {
        return new WebImageSearchService(restClient, SEARCH_URI, clientId, clientSecret, FILTER);
    }

    /** 기대 URL — Client ID/Secret이 들어가지 않는다는 점이 이 문자열의 핵심이다 */
    private static String url(String encodedQuery, int display, int start) {
        return SEARCH_URI + "?query=" + encodedQuery + "&display=" + display
                + "&start=" + start + "&filter=" + FILTER;
    }

    private void expectSearch(String encodedQuery, int display, int start, String responseJson) {
        server.expect(requestTo(url(encodedQuery, display, start)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-NCP-APIGW-API-KEY-ID", CLIENT_ID))
                .andExpect(header("X-NCP-APIGW-API-KEY", CLIENT_SECRET))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
    }

    private static void assertRejectedWith(ImageErrorCode expected, ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(ImageException.class)
                .satisfies(e -> assertThat(((ImageException) e).getCode()).isEqualTo(expected));
    }

    private static String item(String link, String thumbnail) {
        return """
                {
                  "title": "생일 <b>케이크</b> 사진",
                  "link": "%s",
                  "thumbnail": "%s",
                  "sizeheight": "600",
                  "sizewidth": "800"
                }
                """.formatted(link, thumbnail);
    }

    // ------------------------------------------------------------ 정상 경로

    @Test
    @DisplayName("검색 결과를 imageUrl/thumbnailUrl/sourceUrl/alt로 매핑한다 (alt는 <b> 태그 제거)")
    void mapsNaverItemToResponse() {
        expectSearch("perfume", 20, 1, """
                {
                  "total": 100,
                  "start": 1,
                  "display": 20,
                  "items": [%s]
                }
                """.formatted(item("https://img/large.jpg", "https://img/thumb.jpg")));

        WebImageSearchResponse response = service().search("perfume", 0, 20);

        assertThat(response.hasNext()).isTrue(); // start(1)+display(20)=21 <= total(100)
        assertThat(response.images()).hasSize(1);
        WebImageResponse image = response.images().get(0);
        assertThat(image.imageUrl()).isEqualTo("https://img/large.jpg");
        assertThat(image.thumbnailUrl()).isEqualTo("https://img/thumb.jpg");
        // 네이버는 별도의 원본 페이지 링크를 주지 않아 이미지 링크와 동일하다
        assertThat(image.sourceUrl()).isEqualTo("https://img/large.jpg");
        assertThat(image.alt()).isEqualTo("생일 케이크 사진");
        server.verify();
    }

    @Test
    @DisplayName("Client ID/Secret은 URL이 아니라 전용 헤더로 전달된다")
    void sendsCredentialsOnlyInHeaders() {
        // url()에 키가 없으므로, 키가 쿼리로 새면 requestTo 매칭이 실패한다
        expectSearch("perfume", 20, 1, "{\"items\":[]}");

        service().search("perfume", 0, 20);

        server.verify();
    }

    @Test
    @DisplayName("0부터 시작하는 page를 네이버의 1-based start로 변환한다")
    void convertsZeroBasedPageToNaverStart() {
        // page=3, size=15 → start = 3*15+1 = 46
        expectSearch("perfume", 15, 46, "{\"items\":[]}");

        service().search("perfume", 3, 15);

        server.verify();
    }

    @Test
    @DisplayName("한글 검색어는 URL 인코딩되어 전달된다")
    void encodesKoreanQuery() {
        expectSearch("%ED%96%A5%EC%88%98", 20, 1, "{\"items\":[]}");

        service().search("향수", 0, 20);

        server.verify();
    }

    @Test
    @DisplayName("size 미지정 시 기본값 20으로 조회한다")
    void usesDefaultSize() {
        expectSearch("perfume", 20, 1, "{\"items\":[]}");

        service().search("perfume", null, null);

        server.verify();
    }

    // ------------------------------------------------------------ 응답 방어

    @Test
    @DisplayName("thumbnail이 없으면 원본 이미지를 썸네일로 재사용한다")
    void fallsBackToImageUrlWhenThumbnailMissing() {
        expectSearch("perfume", 20, 1, """
                {"items":[{"link":"https://img/large.jpg"}]}
                """);

        WebImageSearchResponse response = service().search("perfume", 0, 20);

        assertThat(response.images().get(0).thumbnailUrl()).isEqualTo("https://img/large.jpg");
    }

    @Test
    @DisplayName("쓸 수 있는 이미지 URL(link)이 없으면 결과에서 제외한다")
    void skipsItemWithoutUsableLink() {
        expectSearch("perfume", 20, 1, """
                {"items":[{"thumbnail":"https://img/thumb.jpg"}]}
                """);

        WebImageSearchResponse response = service().search("perfume", 0, 20);

        assertThat(response.images()).isEmpty();
    }

    @Test
    @DisplayName("items가 없거나 비어 있어도 빈 배열로 정상 응답한다")
    void returnsEmptyListWhenNoItems() {
        expectSearch("perfume", 20, 1, "{}");

        WebImageSearchResponse response = service().search("perfume", 0, 20);

        assertThat(response.images()).isEmpty();
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("start+display가 total 이상이면 hasNext는 false다")
    void hasNextIsFalseWhenNoMoreResults() {
        expectSearch("perfume", 20, 1, """
                {"total": 10, "items":[%s]}
                """.formatted(item("https://img/o.jpg", "https://img/t.jpg")));

        WebImageSearchResponse response = service().search("perfume", 0, 20);

        assertThat(response.hasNext()).isFalse(); // start(1)+display(20)=21 > total(10)
    }

    @Test
    @DisplayName("조회 가능 상한(1000)을 넘는 페이지는 외부 호출 없이 빈 결과를 반환한다")
    void returnsEmptyWithoutCallingApiBeyondReachableLimit() {
        // page=50, size=20 → start = 50*20+1 = 1001 > 1000
        WebImageSearchResponse response = service().search("perfume", 50, 20);

        assertThat(response.images()).isEmpty();
        assertThat(response.hasNext()).isFalse();
        server.verify(); // 기대 요청을 등록하지 않았으므로, 실제로 호출됐다면 여기서 실패한다
    }

    // ------------------------------------------------------------ 실패 분류

    @Test
    @DisplayName("429는 한도 소진(IMAGE429_1)으로 분류한다")
    void classifiesTooManyRequestsAsQuotaExceeded() {
        server.expect(requestTo(url("perfume", 20, 1)))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertRejectedWith(ImageErrorCode.SEARCH_QUOTA_EXCEEDED,
                () -> service().search("perfume", 0, 20));
    }

    @Test
    @DisplayName("401(잘못된 Client ID/Secret)은 사용자 입력 탓이 아니므로 502로 감춘다")
    void classifiesUnauthorizedAsProviderUnavailable() {
        server.expect(requestTo(url("perfume", 20, 1)))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertRejectedWith(ImageErrorCode.SEARCH_PROVIDER_UNAVAILABLE,
                () -> service().search("perfume", 0, 20));
    }

    @Test
    @DisplayName("네이버 5xx는 502로 변환한다")
    void classifiesServerErrorAsProviderUnavailable() {
        server.expect(requestTo(url("perfume", 20, 1)))
                .andRespond(withServerError());

        assertRejectedWith(ImageErrorCode.SEARCH_PROVIDER_UNAVAILABLE,
                () -> service().search("perfume", 0, 20));
    }

    @Test
    @DisplayName("타임아웃도 502로 변환한다 (429와 섞이지 않는다)")
    void classifiesTimeoutAsProviderUnavailable() {
        server.expect(requestTo(url("perfume", 20, 1)))
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
    @ValueSource(ints = {0, -1, 101})
    @DisplayName("size가 1~100 범위를 벗어나면 400으로 거부한다")
    void rejectsInvalidSize(int size) {
        assertRejectedWith(ImageErrorCode.INVALID_PAGE_SIZE,
                () -> service().search("perfume", 0, size));
    }

    @Test
    @DisplayName("음수 page는 0으로 보정한다 (첫 페이지 조회)")
    void normalizesNegativePage() {
        expectSearch("perfume", 20, 1, "{\"items\":[]}");

        service().search("perfume", -5, 20);

        server.verify();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("Client ID/Secret이 없으면 외부 호출조차 하지 않고 503으로 거부한다 (fail-fast)")
    void rejectsWhenCredentialsMissing(String value) {
        String credential = value == null ? "" : value; // @Value 기본값이 빈 문자열이라 null은 오지 않는다
        assertRejectedWith(ImageErrorCode.SEARCH_NOT_CONFIGURED,
                () -> service(credential, CLIENT_SECRET).search("perfume", 0, 20));
        assertRejectedWith(ImageErrorCode.SEARCH_NOT_CONFIGURED,
                () -> service(CLIENT_ID, credential).search("perfume", 0, 20));
        server.verify();
    }
}
