package com.example.toget.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 모든 API 요청 및 응답(반환값)과 소요 시간을 측정하여 로그에 기록하는 필터.
 *
 * [주요 기능]
 * 1. HTTP Method, Request URI, Query Parameter, Status Code, Execution Time(ms) 기록
 * 2. 요청 및 응답 바디(Response Body) 캡처 및 로그 출력
 * 3. Actuator 및 Swagger 등 정적/메트릭 경로는 바디 로깅 대상에서 제외하여 로그 폭증 방지
 * 4. ContentCachingResponseWrapper 사용 후 copyBodyToResponse()를 항상 호출해 응답 전송 보장
 */
@Component
@Slf4j
public class ApiLoggingFilter extends OncePerRequestFilter {

    private static final int MAX_PAYLOAD_LENGTH = 1000;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        ContentCachingRequestWrapper requestWrapper = request instanceof ContentCachingRequestWrapper
                ? (ContentCachingRequestWrapper) request
                : new ContentCachingRequestWrapper(request, MAX_PAYLOAD_LENGTH);

        ContentCachingResponseWrapper responseWrapper = response instanceof ContentCachingResponseWrapper
                ? (ContentCachingResponseWrapper) response
                : new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logApiCall(requestWrapper, responseWrapper, duration);
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logApiCall(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, long duration) {
        String uri = request.getRequestURI();

        // 메트릭, 문서화, static 파일 경로는 상세 로그 생략
        if (isExcludedPath(uri)) {
            return;
        }

        String method = request.getMethod();
        String queryString = request.getQueryString();
        String fullUri = StringUtils.hasText(queryString) ? uri + "?" + queryString : uri;
        int status = response.getStatus();

        String requestBody = getPayload(request.getContentAsByteArray());
        String responseBody = getPayload(response.getContentAsByteArray());

        log.info("[API LOG] {} {} | Status: {} | Duration: {}ms | ReqBody: {} | ResBody: {}",
                method, fullUri, status, duration,
                StringUtils.hasText(requestBody) ? requestBody : "EMPTY",
                StringUtils.hasText(responseBody) ? responseBody : "EMPTY");
    }

    private boolean isExcludedPath(String uri) {
        return uri.startsWith("/actuator")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/swagger-resources")
                || uri.endsWith(".ico")
                || uri.endsWith(".css")
                || uri.endsWith(".js");
    }

    private String getPayload(byte[] buf) {
        if (buf == null || buf.length == 0) {
            return "";
        }
        int length = Math.min(buf.length, MAX_PAYLOAD_LENGTH);
        String payload = new String(buf, 0, length, StandardCharsets.UTF_8);
        return payload.replaceAll("[\\r\\n\\t]", " ");
    }
}
