package com.example.toget.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 은행 아이콘 위치 설정 — application.yaml의 bank-icon.* 값을 바인딩한다.
 *
 * <p>[왜 URL을 통째로 시드하지 않고 base-url만 두는가]
 * 아이콘 파일은 S3에 올라가는데 버킷이 dev/prod로 다르고, 규격이 바뀌면 경로 버전(v1 → v2)도 바뀐다.
 * 전체 URL을 코드나 시드 데이터에 하드코딩하면 환경마다 다른 값을 유지해야 하므로,
 * "베이스 URL(환경변수) + 은행 코드"로 조합한다.
 *
 * <p>미설정 시 아이콘 URL은 null로 남는다. 아이콘 파일 업로드 전에도 스키마·API 작업을
 * 먼저 진행할 수 있도록 한 것이며, 프론트는 null일 때 fallback 아이콘을 표시한다.
 *
 * @param baseUrl  아이콘이 올라간 S3 디렉터리 URL (예: https://버킷.s3.리전.amazonaws.com/bank-icons/v1)
 * @param extension 아이콘 파일 확장자. 기본 svg.
 */
@ConfigurationProperties(prefix = "bank-icon")
public record BankIconProperties(String baseUrl, String extension) {

    public BankIconProperties {
        if (extension == null || extension.isBlank()) {
            extension = "svg";
        }
    }

    /** 아이콘 URL을 만들 수 있는 상태인지 — 미설정이면 시더가 icon_url을 null로 둔다 */
    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank();
    }

    /**
     * 은행 코드로 아이콘 URL을 조합한다. 미설정 시 null.
     * 예: {@code https://.../bank-icons/v1} + {@code KAKAO_BANK} → {@code https://.../bank-icons/v1/KAKAO_BANK.svg}
     */
    public String resolveUrl(String bankCode) {
        if (!isConfigured()) {
            return null;
        }
        String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return trimmed + "/" + bankCode + "." + extension;
    }
}
