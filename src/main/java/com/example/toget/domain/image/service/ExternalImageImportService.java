package com.example.toget.domain.image.service;

import com.example.toget.domain.image.dto.ImageImportRequest;
import com.example.toget.domain.image.dto.ImageImportResponse;
import com.example.toget.domain.image.exception.ImageException;
import com.example.toget.domain.image.exception.code.ImageErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 이미지 가져오기 — 외부 이미지 URL을 서버가 내려받아 우리 S3에 재업로드한다. (issue #102)
 *
 * <p>[왜 필요한가] 웹 사진 검색 결과 URL을 DB에 그대로 저장하면
 * 원본 사이트가 이미지를 내리거나 핫링크를 차단하는 순간 사용자 프로필/선물 이미지가 깨진다.
 * {@code http://} URL은 HTTPS 프론트에서 mixed content로 아예 로드되지 않기도 한다.
 * 선택 시점에 우리 S3로 복사해 두면 이후 원본과 무관하게 안정적으로 서빙된다.
 *
 * <p>[SSRF 방어] "서버가 사용자가 준 URL로 요청을 보낸다"는 것은 그 자체로 SSRF 표면이다.
 * 아래를 모두 막는다.
 * <ul>
 *   <li>http/https 이외의 스킴 (file:, gopher:, ftp: …)</li>
 *   <li>루프백·사설망·링크로컬(169.254.169.254 = 클라우드 메타데이터)·멀티캐스트 주소</li>
 *   <li>자동 리다이렉트 — 직접 최대 {@value #MAX_REDIRECTS}회 따라가되 매 홉마다 위 검사를 다시 한다.
 *       HttpClient의 자동 리다이렉트를 켜면 첫 홉만 검증되고 그 다음은 무방비가 된다.</li>
 *   <li>응답 본문 크기 — Content-Length를 신뢰하지 않고 읽는 도중에 끊는다.</li>
 *   <li>Content-Type 화이트리스트 — SVG는 스크립트를 품을 수 있어 <b>의도적으로 제외</b>한다.
 *       우리 S3 도메인에서 서빙되는 SVG는 저장형 XSS가 된다.</li>
 * </ul>
 * <p>남는 위험은 DNS 리바인딩(검사 후 연결 사이에 응답이 바뀌는 경우)이다. 완전 차단은
 * 소켓 레벨 제어가 필요해 이번 범위에서는 제외했고, S3 업로드 결과 외에는 응답 본문을
 * 사용자에게 되돌려주지 않으므로 정보 유출 경로는 제한적이다.
 */
@Slf4j
@Service
public class ExternalImageImportService {

    private static final int MAX_REDIRECTS = 3;
    private static final long MAX_BYTES = 10L * 1024 * 1024; // 10MB
    private static final String DEFAULT_PREFIX = "web-images";

    /** Content-Type → 확장자. SVG는 저장형 XSS 위험으로 의도적으로 제외했다. */
    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/pjpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif",
            "image/heic", "heic",
            "image/heif", "heif"
    );

    private final S3Client s3Client;
    private final HttpClient httpClient;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    public ExternalImageImportService(S3Client s3Client) {
        this.s3Client = s3Client;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                // 리다이렉트를 직접 처리해 매 홉마다 SSRF 검사를 다시 하기 위해 NEVER로 둔다
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public ImageImportResponse importImage(ImageImportRequest request) {
        URI uri = parseAndValidate(request.sourceImageUrl());

        Downloaded downloaded = download(uri);
        String key = createKey(request.prefix(), downloaded.extension());

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(downloaded.contentType())
                            .contentLength((long) downloaded.bytes().length)
                            .build(),
                    RequestBody.fromBytes(downloaded.bytes())
            );
        } catch (RuntimeException e) {
            log.error("이미지 가져오기 S3 업로드 실패: key={}", key, e);
            throw new ImageException(ImageErrorCode.IMPORT_UPLOAD_FAILED);
        }

        return new ImageImportResponse(
                String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key));
    }

    // ---------------------------------------------------------------- URL 검증

    private URI parseAndValidate(String rawUrl) {
        URI uri;
        try {
            uri = new URI(rawUrl.trim());
        } catch (URISyntaxException | NullPointerException e) {
            throw new ImageException(ImageErrorCode.IMPORT_INVALID_URL);
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new ImageException(ImageErrorCode.IMPORT_INVALID_URL);
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new ImageException(ImageErrorCode.IMPORT_INVALID_URL);
        }
        requirePublicHost(uri.getHost());
        return uri;
    }

    /**
     * 호스트가 가리키는 모든 주소가 공인 IP인지 확인한다.
     * 하나라도 내부망이면 거부 — 어떤 A 레코드로 연결될지 보장할 수 없기 때문이다.
     */
    private void requirePublicHost(String host) {
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new ImageException(ImageErrorCode.IMPORT_INVALID_URL);
        }
        for (InetAddress address : addresses) {
            if (address.isAnyLocalAddress()      // 0.0.0.0
                    || address.isLoopbackAddress()   // 127.0.0.0/8, ::1
                    || address.isLinkLocalAddress()  // 169.254.0.0/16 — AWS/GCP 메타데이터 엔드포인트
                    || address.isSiteLocalAddress()  // 10/8, 172.16/12, 192.168/16
                    || address.isMulticastAddress()
                    || isUniqueLocalIpv6(address)) {
                log.warn("이미지 가져오기 차단 — 내부 주소 요청: host={}", host);
                throw new ImageException(ImageErrorCode.IMPORT_BLOCKED_HOST);
            }
        }
    }

    /** IPv6 Unique Local Address(fc00::/7) — isSiteLocalAddress()가 잡아주지 않는다. */
    private boolean isUniqueLocalIpv6(InetAddress address) {
        byte[] bytes = address.getAddress();
        return bytes.length == 16 && (bytes[0] & 0xFE) == 0xFC;
    }

    // ---------------------------------------------------------------- 다운로드

    private Downloaded download(URI uri) {
        URI current = uri;
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            HttpResponse<InputStream> response = send(current);
            int status = response.statusCode();

            if (status >= 300 && status < 400) {
                String location = response.headers().firstValue("location").orElse(null);
                if (location == null) {
                    throw new ImageException(ImageErrorCode.IMPORT_FETCH_FAILED);
                }
                // 상대 경로 Location도 있으므로 현재 URI 기준으로 해석한 뒤 다시 검증한다
                current = parseAndValidate(current.resolve(location).toString());
                continue;
            }
            if (status != 200) {
                log.warn("이미지 가져오기 실패 — 원본 응답 {}: {}", status, current);
                throw new ImageException(ImageErrorCode.IMPORT_FETCH_FAILED);
            }
            return readBody(response);
        }
        throw new ImageException(ImageErrorCode.IMPORT_FETCH_FAILED);
    }

    private HttpResponse<InputStream> send(URI uri) {
        HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(Duration.ofSeconds(7))
                // User-Agent가 없으면 봇으로 보고 403을 주는 사이트가 많다
                .header("User-Agent", "Mozilla/5.0 (compatible; TogetBot/1.0)")
                .header("Accept", "image/*")
                .build();
        try {
            return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            log.warn("이미지 가져오기 네트워크 실패: {}", uri, e);
            throw new ImageException(ImageErrorCode.IMPORT_FETCH_FAILED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 인터럽트 상태를 삼키지 않는다
            throw new ImageException(ImageErrorCode.IMPORT_FETCH_FAILED);
        }
    }

    private Downloaded readBody(HttpResponse<InputStream> response) {
        String contentType = response.headers().firstValue("content-type")
                .map(v -> v.split(";")[0].trim().toLowerCase(Locale.ROOT))
                .orElse("");
        String extension = ALLOWED_CONTENT_TYPES.get(contentType);
        if (extension == null) {
            throw new ImageException(ImageErrorCode.IMPORT_UNSUPPORTED_CONTENT_TYPE);
        }
        // Content-Length는 거짓일 수 있지만, 명백히 초과라면 본문을 읽기 전에 끊는 편이 싸다
        response.headers().firstValueAsLong("content-length").ifPresent(length -> {
            if (length > MAX_BYTES) {
                throw new ImageException(ImageErrorCode.IMPORT_TOO_LARGE);
            }
        });

        try (InputStream in = response.body()) {
            // MAX_BYTES + 1까지만 읽어서, 한 바이트라도 넘치면 초과로 판정한다.
            // readAllBytes()를 쓰면 헤더를 속인 거대 응답에 힙이 그대로 노출된다.
            byte[] bytes = in.readNBytes((int) MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) {
                throw new ImageException(ImageErrorCode.IMPORT_TOO_LARGE);
            }
            if (bytes.length == 0) {
                throw new ImageException(ImageErrorCode.IMPORT_FETCH_FAILED);
            }
            return new Downloaded(bytes, contentType, extension);
        } catch (IOException e) {
            throw new ImageException(ImageErrorCode.IMPORT_FETCH_FAILED);
        }
    }

    // ---------------------------------------------------------------- S3 키

    /**
     * 확장자는 원본 파일명이 아니라 <b>응답 Content-Type</b>에서 뽑는다.
     * 원본 경로의 확장자를 믿으면 사용자가 준 문자열이 그대로 S3 키에 섞여 들어간다.
     */
    private String createKey(String prefix, String extension) {
        String cleanPrefix = (prefix == null || prefix.isBlank())
                ? DEFAULT_PREFIX
                : prefix.replaceAll("[^a-zA-Z0-9/_-]", "").replaceAll("^[./]+", "").replaceAll("/+", "/");
        if (cleanPrefix.isBlank()) {
            cleanPrefix = DEFAULT_PREFIX;
        }
        return cleanPrefix + "/" + UUID.randomUUID() + "." + extension;
    }

    private record Downloaded(byte[] bytes, String contentType, String extension) {
    }
}
