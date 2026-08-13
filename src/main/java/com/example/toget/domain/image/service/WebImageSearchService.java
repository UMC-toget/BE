package com.example.toget.domain.image.service;

import com.example.toget.domain.image.dto.WebImageResponse;
import com.example.toget.domain.image.dto.WebImageSearchResponse;
import com.example.toget.domain.image.exception.ImageException;
import com.example.toget.domain.image.exception.code.ImageErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 웹 사진 검색 프록시 — 네이버 검색(이미지) API 호출을 서버가 대신한다.
 *
 * <p>[왜 구글이 아니라 네이버인가]
 * 처음에는 구글 Custom Search JSON API로 구현했으나, 해당 API는 <b>신규 고객에게 닫혀 있고</b>
 * (호출 시 403 "This project does not have the access to Custom Search JSON API"),
 * 기존 고객도 2027-01-01에 서비스가 종료된다. 이후 Pexels로 교체했다가, 검색 결과가
 * 스톡 사진으로 한정돼 실제 상품/브랜드 사진이 거의 안 나오는 문제로 다시 네이버로 교체했다.
 *
 * <p>[NAVER API HUB로 발급 경로 변경] 네이버 검색 API는 2026-07-31부로 기존 네이버
 * 개발자센터(openapi.naver.com)에서의 신규 발급이 막히고, NAVER Cloud Platform(NCP)의
 * NAVER API HUB로 이관됐다. 이 때문에 엔드포인트와 인증 헤더가 개발자센터 시절과 다르다
 * (아래 [인증] 참고). 요청/응답 파라미터(query/display/start/filter, items[].link 등)는
 * 동일하게 유지된다. 참고: https://api.ncloud-docs.com/docs/naver-api-hub-search-image
 *
 * <p>[저작권 주의] 네이버 이미지 검색은 Pexels 같은 무료 라이선스 스톡 사진 사이트가 아니라
 * 웹을 크롤링한 임의의 제3자 이미지를 그대로 색인한 결과다. 검색 API 이용약관이 검색 결과의
 * 재배포·복제까지 허용하는 것은 아니므로, 이 결과를 S3로 복제하는 것(ExternalImageImportService)은
 * 저작권 리스크가 있다는 점을 인지한 상태로 사용한다 — 프로덕트 판단으로 계속 진행한다.
 * (Pexels 버전에 있던 사진작가 표기 요구사항은 없다 — 스톡 사진이 아니므로 해당 사항 자체가 없다.)
 *
 * <p>[인증] NAVER API HUB는 애플리케이션의 Client ID/Secret을
 * X-NCP-APIGW-API-KEY-ID / X-NCP-APIGW-API-KEY 헤더로 받는다
 * (구 개발자센터의 X-Naver-Client-Id/Secret과는 다른 값·헤더명이다).
 *
 * <p>[페이지네이션] 네이버의 start는 1부터 시작하고 display(=size)는 최대 100,
 * start+display가 1000을 넘는 조회는 네이버 API 자체가 지원하지 않는다.
 * 프론트 계약(0-based page)은 그대로 두고 여기서 변환한다.
 */
@Slf4j
@Service
public class WebImageSearchService {

    /** 네이버 이미지 검색 display 상한 */
    private static final int MAX_SIZE = 100;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_QUERY_LENGTH = 100;
    /** 네이버 이미지 검색은 start + display가 이 값을 넘는 조회를 지원하지 않는다 */
    private static final int MAX_REACHABLE_RESULT = 1000;

    private final RestClient restClient;
    private final String searchUri;
    private final String clientId;
    private final String clientSecret;
    private final String filter;

    public WebImageSearchService(RestClient imageSearchRestClient,
                                 @Value("${image-search.naver.uri:https://naverapihub.apigw.ntruss.com/search/v1/image}") String searchUri,
                                 @Value("${image-search.naver.client-id:}") String clientId,
                                 @Value("${image-search.naver.client-secret:}") String clientSecret,
                                 @Value("${image-search.naver.filter:large}") String filter) {
        this.restClient = imageSearchRestClient;
        this.searchUri = searchUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.filter = filter;
    }

    /**
     * @param query 검색어
     * @param page  0부터 시작하는 페이지 번호 (네이버의 1-based start로 변환된다)
     * @param size  페이지당 개수 (1~100, 기본 20)
     */
    public WebImageSearchResponse search(String query, Integer page, Integer size) {
        String q = normalizeQuery(query);
        int pageNumber = (page == null || page < 0) ? 0 : page;
        int pageSize = normalizeSize(size);

        requireConfigured();

        int start = pageNumber * pageSize + 1; // 0-based page → 네이버 1-based start
        if (start > MAX_REACHABLE_RESULT) {
            // 네이버 API 자체가 조회할 수 없는 범위 — 외부 호출 없이 빈 결과로 처리한다
            return WebImageSearchResponse.empty();
        }
        int display = Math.min(pageSize, MAX_REACHABLE_RESULT - start + 1);

        JsonNode body = call(q, start, display);
        return new WebImageSearchResponse(toImages(body.get("items")), hasNext(body, start, display));
    }

    // ---------------------------------------------------------------- 요청 검증

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank() || query.trim().length() > MAX_QUERY_LENGTH) {
            throw new ImageException(ImageErrorCode.INVALID_SEARCH_QUERY);
        }
        return query.trim();
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new ImageException(ImageErrorCode.INVALID_PAGE_SIZE);
        }
        return size;
    }

    /** 키가 없으면 어떤 응답도 받을 수 없으므로 외부 호출 전에 즉시 거부한다. */
    private void requireConfigured() {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            log.warn("이미지 검색 설정 누락 — NAVER_SEARCH_CLIENT_ID/NAVER_SEARCH_CLIENT_SECRET 확인 필요");
            throw new ImageException(ImageErrorCode.SEARCH_NOT_CONFIGURED);
        }
    }

    // ---------------------------------------------------------------- 외부 호출

    private JsonNode call(String query, int start, int display) {
        ResponseEntity<JsonNode> response;
        try {
            response = restClient.get()
                    // 템플릿 변수는 URL 인코딩되어 치환된다 (한글 검색어도 안전)
                    .uri(searchUri + "?query={q}&display={display}&start={start}&filter={filter}",
                            query, display, start, filter)
                    // 키는 헤더로만 전달한다 — URI에 넣으면 예외 메시지·접근 로그로 샌다
                    .header("X-NCP-APIGW-API-KEY-ID", clientId)
                    .header("X-NCP-APIGW-API-KEY", clientSecret)
                    .retrieve()
                    .toEntity(JsonNode.class);
        } catch (HttpClientErrorException e) {
            throw new ImageException(classify4xx(e));
        } catch (RuntimeException e) {
            // 5xx, 타임아웃, DNS 실패, 본문 파싱 실패 등
            log.warn("네이버 이미지 검색 호출 실패: {} - {}",
                    e.getClass().getSimpleName(), redact(e.getMessage()));
            throw new ImageException(ImageErrorCode.SEARCH_PROVIDER_UNAVAILABLE);
        }
        JsonNode body = response.getBody();
        if (body == null) {
            // status는 2xx인데 파싱된 body가 없는 경우 — 예외가 안 나서 위 catch들로는 안 잡힌다
            log.warn("네이버 이미지 검색 응답 본문 없음 (status={}, content-type={})",
                    response.getStatusCode(), response.getHeaders().getContentType());
            throw new ImageException(ImageErrorCode.SEARCH_PROVIDER_UNAVAILABLE);
        }
        return body;
    }

    /**
     * 429(일일 한도 소진)만 사용자에게 "한도 초과"로 알리고, 나머지 4xx는 우리 설정 문제이므로
     * 502로 감춘다. 401/403은 Client ID/Secret이 잘못된 경우인데, 이를 400으로 돌려주면
     * 사용자가 검색어를 고치려 들게 되므로 적절치 않다.
     */
    private ImageErrorCode classify4xx(HttpClientErrorException e) {
        int status = e.getStatusCode().value();
        if (status == HttpStatus.TOO_MANY_REQUESTS.value()) {
            return ImageErrorCode.SEARCH_QUOTA_EXCEEDED;
        }
        log.warn("네이버 이미지 검색 4xx 응답: status={}, body={}",
                status, redact(e.getResponseBodyAsString()));
        return ImageErrorCode.SEARCH_PROVIDER_UNAVAILABLE;
    }

    // ---------------------------------------------------------------- 응답 변환

    /**
     * items → 응답 DTO. 결과가 0건이면 items가 빈 배열로 오지만 null/비배열도 함께 방어한다.
     *
     * <p>네이버 이미지 검색은 원본(link) 하나만 내려주고 Pexels처럼 해상도별 변형을 제공하지
     * 않는다. thumbnail이 없으면 원본을 그대로 썸네일로 재사용한다.
     */
    private List<WebImageResponse> toImages(JsonNode items) {
        if (items == null || !items.isArray()) {
            return List.of();
        }
        List<WebImageResponse> images = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            JsonNode item = items.get(i);
            String imageUrl = textOrNull(item, "link");
            if (imageUrl == null) { // 쓸 수 있는 URL이 없으면 적용 자체가 불가하므로 버린다
                continue;
            }
            String thumbnailUrl = textOrNull(item, "thumbnail");
            images.add(new WebImageResponse(
                    imageUrl,
                    thumbnailUrl != null ? thumbnailUrl : imageUrl,
                    imageUrl, // 네이버는 별도의 '원본 페이지' 링크를 주지 않아 이미지 링크로 대신한다
                    stripHtmlTags(textOrNull(item, "title"))
            ));
        }
        return images;
    }

    /**
     * 네이버는 전체 결과 수(total)만 내려주고 Pexels처럼 "다음 페이지 존재" 플래그를 주지 않는다.
     * 이번 페이지로 다 못 봤고, 조회 상한(1000)에도 걸리지 않았을 때만 다음 페이지가 있다고 본다.
     */
    private boolean hasNext(JsonNode body, int start, int display) {
        JsonNode totalNode = body.get("total");
        long total = totalNode != null && totalNode.isNumber() ? totalNode.asLong() : 0;
        int nextStart = start + display;
        return nextStart <= total && nextStart <= MAX_REACHABLE_RESULT;
    }

    /** 네이버 title에는 검색어 강조용 <b> 태그가 섞여 온다 — alt 텍스트로 쓰기 전에 제거한다 */
    private static String stripHtmlTags(String value) {
        return value == null ? null : value.replaceAll("<[^>]*>", "");
    }

    /** 필드가 없거나 null이면 null. (Jackson 3에서 path()+MissingNode 조합은 예외를 던지므로 get() 사용) */
    private static String textOrNull(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        return (value == null || value.isNull()) ? null : value.asString();
    }

    /**
     * 로그로 나가는 문자열에서 자격증명을 가린다.
     * 네이버는 키를 헤더로 받으므로 URI/응답 본문에 키가 섞일 일은 없지만,
     * 호출 방식이 바뀌어도 로그가 새지 않도록 한 겹 남겨 둔다.
     */
    private static String redact(String message) {
        if (message == null) {
            return "";
        }
        return message.replaceAll("(?i)(client-secret|client-id|authorization)=[^&\"\\s]+", "$1=***");
    }
}
