package com.example.toget.domain.image.service;

import com.example.toget.domain.image.dto.WebImageResponse;
import com.example.toget.domain.image.dto.WebImageSearchResponse;
import com.example.toget.domain.image.exception.ImageException;
import com.example.toget.domain.image.exception.code.ImageErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 웹 사진 검색 프록시 — 구글 Custom Search JSON API(searchType=image) 호출을 서버가 대신한다.
 *
 * <p>[왜 프록시인가] Custom Search는 API 키가 필요한데, 프론트 코드에 키를 넣으면 그대로 노출되어
 * 제3자가 우리 쿼터(무료 기준 일 100건)를 소진시킬 수 있다. 키는 서버 환경변수로만 들고,
 * 프론트는 우리 엔드포인트만 부른다.
 *
 * <p>[페이지네이션] 구글은 0-based page가 아니라 1-based start 인덱스를 쓰고,
 * num은 1~10, start + num <= 101 이라는 하드 제약이 있다(= 최대 100건까지만 조회 가능).
 * 프론트 친화적인 page/size를 받아 여기서 start로 변환하고, 범위를 넘으면 400으로 끊는다.
 *
 * <p>[실패 분류] 쿼터 소진(429/403 dailyLimitExceeded)은 429로, 구글 장애·타임아웃은 502로 나간다.
 * 이 둘을 뭉뚱그리면 프론트가 "오늘은 직접 업로드해주세요" 안내를 띄울지,
 * "잠시 후 재시도"를 띄울지 구분할 수 없다.
 */
@Slf4j
@Service
public class WebImageSearchService {

    /** 구글 Custom Search가 허용하는 한 페이지 최대 건수 */
    private static final int MAX_SIZE = 10;
    private static final int DEFAULT_SIZE = 10;
    /** start + num <= 101 → 조회 가능한 결과의 총 상한 */
    private static final int MAX_RESULT_INDEX = 100;
    private static final int MAX_QUERY_LENGTH = 100;

    private final RestClient restClient;
    private final String searchUri;
    private final String apiKey;
    private final String cx;
    private final String safeSearch;

    public WebImageSearchService(RestClient imageSearchRestClient,
                                 @Value("${image-search.google.uri:https://www.googleapis.com/customsearch/v1}") String searchUri,
                                 @Value("${image-search.google.api-key:}") String apiKey,
                                 @Value("${image-search.google.cx:}") String cx,
                                 @Value("${image-search.google.safe:active}") String safeSearch) {
        this.restClient = imageSearchRestClient;
        this.searchUri = searchUri;
        this.apiKey = apiKey;
        this.cx = cx;
        this.safeSearch = safeSearch;
    }

    /**
     * @param query 검색어
     * @param page  0부터 시작하는 페이지 번호
     * @param size  페이지당 개수 (1~10, 기본 10)
     */
    public WebImageSearchResponse search(String query, Integer page, Integer size) {
        String q = normalizeQuery(query);
        int pageNumber = (page == null || page < 0) ? 0 : page;
        int pageSize = normalizeSize(size);
        int start = toGoogleStart(pageNumber, pageSize);

        requireConfigured();

        JsonNode body = call(q, start, pageSize);
        return new WebImageSearchResponse(toImages(body.get("items")), hasNext(body, pageNumber, pageSize));
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

    /**
     * 0-based page → 1-based start 변환. 구글이 400을 주기 전에 우리가 먼저 걸러
     * 쓸데없이 쿼터를 소모하지 않도록 한다.
     */
    private int toGoogleStart(int page, int size) {
        long offset = (long) page * size;
        if (offset + size > MAX_RESULT_INDEX) {
            throw new ImageException(ImageErrorCode.SEARCH_RANGE_EXCEEDED);
        }
        return (int) offset + 1;
    }

    /** 키/CX가 없으면 어떤 응답도 받을 수 없으므로 외부 호출 전에 즉시 거부한다. */
    private void requireConfigured() {
        if (apiKey.isBlank() || cx.isBlank()) {
            log.warn("이미지 검색 설정 누락 — IMAGE_SEARCH_API_KEY / IMAGE_SEARCH_CX 확인 필요");
            throw new ImageException(ImageErrorCode.SEARCH_NOT_CONFIGURED);
        }
    }

    // ---------------------------------------------------------------- 외부 호출

    private JsonNode call(String query, int start, int size) {
        JsonNode body;
        try {
            body = restClient.get()
                    // 템플릿 변수는 URL 인코딩되어 치환된다 (문자열 직접 연결보다 안전)
                    .uri(searchUri + "?key={key}&cx={cx}&q={q}&searchType=image&num={num}&start={start}&safe={safe}",
                            apiKey, cx, query, size, start, safeSearch)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpClientErrorException e) {
            throw new ImageException(classify4xx(e));
        } catch (RuntimeException e) {
            // 5xx, 타임아웃, DNS 실패, 본문 파싱 실패 등
            log.warn("구글 이미지 검색 호출 실패", e);
            throw new ImageException(ImageErrorCode.SEARCH_PROVIDER_UNAVAILABLE);
        }
        if (body == null) {
            throw new ImageException(ImageErrorCode.SEARCH_PROVIDER_UNAVAILABLE);
        }
        return body;
    }

    /**
     * 구글은 쿼터 소진을 429로도, 403(reason=dailyLimitExceeded/rateLimitExceeded)으로도 응답한다.
     * 둘 다 429(SEARCH_QUOTA_EXCEEDED)로 통일해 프론트가 한 가지 분기만 하면 되게 한다.
     */
    private ImageErrorCode classify4xx(HttpClientErrorException e) {
        int status = e.getStatusCode().value();
        if (status == HttpStatus.TOO_MANY_REQUESTS.value()) {
            return ImageErrorCode.SEARCH_QUOTA_EXCEEDED;
        }
        if (status == HttpStatus.FORBIDDEN.value() && isQuotaReason(e.getResponseBodyAsString())) {
            return ImageErrorCode.SEARCH_QUOTA_EXCEEDED;
        }
        // 잘못된 키/CX 등 우리 설정 문제 — 사용자 입력 탓이 아니므로 400으로 돌려주지 않는다
        log.warn("구글 이미지 검색 4xx 응답: status={}, body={}", status, e.getResponseBodyAsString());
        return ImageErrorCode.SEARCH_PROVIDER_UNAVAILABLE;
    }

    private boolean isQuotaReason(String responseBody) {
        if (responseBody == null) {
            return false;
        }
        return responseBody.contains("dailyLimitExceeded")
                || responseBody.contains("rateLimitExceeded")
                || responseBody.contains("quotaExceeded");
    }

    // ---------------------------------------------------------------- 응답 변환

    /**
     * items → 응답 DTO. 구글은 결과가 0건이면 items 자체를 생략하므로 null/비배열을 방어한다.
     * thumbnailLink가 없으면 imageUrl을 재사용해 프론트가 항상 무언가는 렌더링할 수 있게 한다.
     */
    private List<WebImageResponse> toImages(JsonNode items) {
        if (items == null || !items.isArray()) {
            return List.of();
        }
        List<WebImageResponse> images = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            JsonNode item = items.get(i);
            String imageUrl = textOrNull(item, "link");
            if (imageUrl == null) { // 원본 URL이 없으면 적용 자체가 불가하므로 버린다
                continue;
            }
            JsonNode image = item.get("image");
            String thumbnailUrl = textOrNull(image, "thumbnailLink");
            images.add(new WebImageResponse(
                    imageUrl,
                    thumbnailUrl != null ? thumbnailUrl : imageUrl,
                    textOrNull(image, "contextLink")
            ));
        }
        return images;
    }

    /**
     * 다음 페이지 존재 여부. 구글이 queries.nextPage를 내려주면 다음 페이지가 있다.
     *
     * <p>다만 nextPage는 100건 상한을 넘어서도 채워지므로, 다음 페이지 요청이 실제로 통과할지를
     * 우리 상한으로 한 번 더 확인한다. 이게 없으면 프론트가 hasNext를 믿고 스크롤했다가
     * 곧바로 400(SEARCH_RANGE_EXCEEDED)을 맞는다.
     */
    private boolean hasNext(JsonNode body, int page, int size) {
        JsonNode queries = body.get("queries");
        if (queries == null) {
            return false;
        }
        JsonNode nextPage = queries.get("nextPage");
        if (nextPage == null || !nextPage.isArray() || nextPage.size() == 0) {
            return false;
        }
        return (long) (page + 1) * size + size <= MAX_RESULT_INDEX;
    }

    /** 필드가 없거나 null이면 null. (Jackson 3에서 path()+MissingNode 조합은 예외를 던지므로 get() 사용) */
    private static String textOrNull(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        return (value == null || value.isNull()) ? null : value.asString();
    }
}
