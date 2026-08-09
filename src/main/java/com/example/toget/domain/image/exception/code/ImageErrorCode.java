package com.example.toget.domain.image.exception.code;

import com.example.toget.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ImageErrorCode implements BaseErrorCode {

    INVALID_SEARCH_QUERY(HttpStatus.BAD_REQUEST, "IMAGE400_1", "검색어는 1자 이상 100자 이하여야 합니다."),
    // 구글 Custom Search는 start + num <= 101 제약이 있어 조회 가능한 범위를 벗어나면 400으로 끊는다.
    SEARCH_RANGE_EXCEEDED(HttpStatus.BAD_REQUEST, "IMAGE400_2", "조회 가능한 검색 범위를 벗어났습니다. (최대 100건)"),
    INVALID_PAGE_SIZE(HttpStatus.BAD_REQUEST, "IMAGE400_3", "페이지당 개수는 1 이상 10 이하여야 합니다."),

    // ---- 이미지 가져오기(외부 URL → S3 재업로드) ----
    IMPORT_INVALID_URL(HttpStatus.BAD_REQUEST, "IMAGE400_4", "가져올 수 없는 이미지 주소입니다."),
    IMPORT_UNSUPPORTED_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "IMAGE400_5", "지원하지 않는 이미지 형식입니다."),
    // SSRF 차단 — 사설망/루프백/메타데이터 주소로의 요청은 사용자에게 이유를 알리지 않고 거부한다
    IMPORT_BLOCKED_HOST(HttpStatus.BAD_REQUEST, "IMAGE400_6", "가져올 수 없는 이미지 주소입니다."),
    IMPORT_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE413_1", "이미지 용량이 너무 큽니다. (최대 10MB)"),

    // 하루 무료 쿼터(100건) 소진 — 프론트는 이 코드를 받으면 "직접 업로드"로 안내한다.
    SEARCH_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "IMAGE429_1", "오늘의 웹 사진 검색 한도를 모두 사용했습니다. 잠시 후 다시 시도하거나 직접 업로드해주세요."),

    // 구글 장애·타임아웃 — 우리 잘못도, 요청 잘못도 아니므로 502
    SEARCH_PROVIDER_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "IMAGE502_1", "이미지 검색 서비스에 일시적으로 연결할 수 없습니다."),
    // 원본 사이트가 죽었거나 핫링크를 차단한 경우 — 프론트는 "다른 이미지를 선택해주세요"로 안내
    IMPORT_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "IMAGE502_2", "이미지를 가져오지 못했습니다. 다른 이미지를 선택해주세요."),

    // S3 업로드 실패 — 우리 인프라 문제
    IMPORT_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE500_1", "이미지 저장에 실패했습니다."),

    // API 키/CX 미설정 — 설정 누락 상태에서 외부 호출을 시도하지 않고 즉시 끊는다(fail-fast)
    SEARCH_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "IMAGE503_1", "이미지 검색 기능이 설정되지 않았습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
