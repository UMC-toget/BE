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
 * 웹 사진 검색 프록시 — Pexels API 호출을 서버가 대신한다.
 *
 * <p>[왜 구글이 아니라 Pexels인가]
 * 처음에는 구글 Custom Search JSON API로 구현했으나, 해당 API는 <b>신규 고객에게 닫혀 있고</b>
 * (호출 시 403 "This project does not have the access to Custom Search JSON API"),
 * 기존 고객도 2027-01-01에 서비스가 종료된다. 새로 붙일 수 없는 API라 Pexels로 교체했다.
 *
 * <p>부수적으로 저작권 문제도 정리된다. 구글 이미지 검색 결과는 제3자의 저작물이라
 * 그것을 우리 S3로 복제하는 것(ImageImportService)이 걸리는데, Pexels 사진은 무료 라이선스라
 * 복제·재배포가 허용된다. 대신 <b>사진작가 표기가 라이선스 조건</b>이므로
 * 응답에 photographer / photographerUrl을 함께 내려준다.
 *
 * <p>[키 전달 방식] Pexels는 API 키를 쿼리 파라미터가 아니라 Authorization 헤더로 받는다.
 * 덕분에 예외 메시지에 섞여 들어오는 요청 URI로 키가 새던 문제가 구조적으로 사라졌다.
 * (그래도 방어적으로 redact()는 유지한다)
 *
 * <p>[페이지네이션] Pexels의 page는 1부터 시작하고 per_page는 최대 80이다.
 * 프론트 계약(0-based page)은 그대로 두고 여기서 변환한다.
 * 구글과 달리 100건 상한이 없어 깊은 페이지도 조회할 수 있다.
 */
@Slf4j
@Service
public class WebImageSearchService {

    /** Pexels per_page 상한 */
    private static final int MAX_SIZE = 80;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_QUERY_LENGTH = 100;

    private final RestClient restClient;
    private final String searchUri;
    private final String apiKey;
    private final String locale;

    public WebImageSearchService(RestClient imageSearchRestClient,
                                 @Value("${image-search.pexels.uri:https://api.pexels.com/v1/search}") String searchUri,
                                 @Value("${image-search.pexels.api-key:}") String apiKey,
                                 @Value("${image-search.pexels.locale:ko-KR}") String locale) {
        this.restClient = imageSearchRestClient;
        this.searchUri = searchUri;
        this.apiKey = apiKey;
        this.locale = locale;
    }

    /**
     * @param query 검색어
     * @param page  0부터 시작하는 페이지 번호 (Pexels의 1-based page로 변환된다)
     * @param size  페이지당 개수 (1~80, 기본 20)
     */
    public WebImageSearchResponse search(String query, Integer page, Integer size) {
        String q = normalizeQuery(query);
        int pageNumber = (page == null || page < 0) ? 0 : page;
        int pageSize = normalizeSize(size);

        requireConfigured();

        JsonNode body = call(q, pageNumber + 1, pageSize); // 0-based → 1-based
        return new WebImageSearchResponse(toImages(body.get("photos")), hasNext(body));
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
        if (apiKey.isBlank()) {
            log.warn("이미지 검색 설정 누락 — PEXELS_API_KEY 확인 필요");
            throw new ImageException(ImageErrorCode.SEARCH_NOT_CONFIGURED);
        }
    }

    // ---------------------------------------------------------------- 외부 호출

    private JsonNode call(String query, int page, int perPage) {
        JsonNode body;
        try {
            body = restClient.get()
                    // 템플릿 변수는 URL 인코딩되어 치환된다 (한글 검색어도 안전)
                    .uri(searchUri + "?query={q}&page={page}&per_page={perPage}&locale={locale}",
                            query, page, perPage, locale)
                    // 키는 헤더로만 전달한다 — URI에 넣으면 예외 메시지·접근 로그로 샌다
                    .header("Authorization", apiKey)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpClientErrorException e) {
            throw new ImageException(classify4xx(e));
        } catch (RuntimeException e) {
            // 5xx, 타임아웃, DNS 실패, 본문 파싱 실패 등
            log.warn("Pexels 이미지 검색 호출 실패: {} - {}",
                    e.getClass().getSimpleName(), redact(e.getMessage()));
            throw new ImageException(ImageErrorCode.SEARCH_PROVIDER_UNAVAILABLE);
        }
        if (body == null) {
            throw new ImageException(ImageErrorCode.SEARCH_PROVIDER_UNAVAILABLE);
        }
        return body;
    }

    /**
     * 429(월 쿼터 소진)만 사용자에게 "한도 초과"로 알리고, 나머지 4xx는 우리 설정 문제이므로
     * 502로 감춘다. 401은 키가 잘못된 경우인데, 이를 400으로 돌려주면
     * 사용자가 검색어를 고치려 들게 되므로 적절치 않다.
     */
    private ImageErrorCode classify4xx(HttpClientErrorException e) {
        int status = e.getStatusCode().value();
        if (status == HttpStatus.TOO_MANY_REQUESTS.value()) {
            return ImageErrorCode.SEARCH_QUOTA_EXCEEDED;
        }
        log.warn("Pexels 이미지 검색 4xx 응답: status={}, body={}",
                status, redact(e.getResponseBodyAsString()));
        return ImageErrorCode.SEARCH_PROVIDER_UNAVAILABLE;
    }

    // ---------------------------------------------------------------- 응답 변환

    /**
     * photos → 응답 DTO. 결과가 0건이면 photos가 빈 배열로 오지만 null/비배열도 함께 방어한다.
     *
     * <p>[왜 original이 아니라 large2x인가]
     * Pexels의 original은 손대지 않은 원본이라 5000px/수 MB급이 흔하다. 프로필 사진이나
     * 선물 카드 이미지로는 과하고, 세 가지 문제가 생긴다.
     * <ul>
     *   <li>이미지 가져오기(imports)의 10MB 상한에 걸려 413으로 실패할 수 있다</li>
     *   <li>S3 저장 용량과 전송 비용이 불필요하게 커진다</li>
     *   <li>모바일에서 로딩이 느려진다</li>
     * </ul>
     * large2x(940×650 @2x)면 고해상도 화면에서도 충분하다. 혹시 large2x가 없으면
     * large → original 순으로 폴백한다.
     *
     * <p>thumbnailUrl은 src.medium(높이 350px) — 그리드에 큰 이미지를 수십 장 걸지 않기 위해서다.
     */
    private List<WebImageResponse> toImages(JsonNode photos) {
        if (photos == null || !photos.isArray()) {
            return List.of();
        }
        List<WebImageResponse> images = new ArrayList<>(photos.size());
        for (int i = 0; i < photos.size(); i++) {
            JsonNode photo = photos.get(i);
            JsonNode src = photo.get("src");
            String imageUrl = firstNonNull(
                    textOrNull(src, "large2x"),
                    textOrNull(src, "large"),
                    textOrNull(src, "original"));
            if (imageUrl == null) { // 쓸 수 있는 URL이 하나도 없으면 적용 자체가 불가하므로 버린다
                continue;
            }
            String thumbnailUrl = textOrNull(src, "medium");
            images.add(new WebImageResponse(
                    imageUrl,
                    thumbnailUrl != null ? thumbnailUrl : imageUrl,
                    textOrNull(photo, "url"),              // Pexels 사진 페이지 → '사이트 방문하기'
                    textOrNull(photo, "photographer"),     // 라이선스상 표기 필요
                    textOrNull(photo, "photographer_url"),
                    textOrNull(photo, "alt")
            ));
        }
        return images;
    }

    /**
     * Pexels는 다음 페이지가 있을 때만 next_page를 내려준다 — 존재 여부만 보면 된다.
     *
     * <p>[주의] next_page에 담긴 URL 자체는 쓰지 않는다. 실제 응답에서
     * "https://api.pexels.com/v1/v1/search?..."처럼 경로가 중복되어 오는 것을 확인했다(Pexels 측 버그).
     * 그 URL을 그대로 따라가면 404가 난다.
     */
    private boolean hasNext(JsonNode body) {
        JsonNode nextPage = body.get("next_page");
        return nextPage != null && !nextPage.isNull();
    }

    private static String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
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
     * Pexels는 키를 헤더로 받으므로 URI에 키가 섞일 일은 없지만,
     * 호출 방식이 바뀌어도 로그가 새지 않도록 한 겹 남겨 둔다.
     */
    private static String redact(String message) {
        if (message == null) {
            return "";
        }
        return message.replaceAll("(?i)(key|authorization)=[^&\"\\s]+", "$1=***");
    }
}
