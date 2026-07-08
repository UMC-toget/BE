package com.example.toget.domain.user.service;

import com.example.toget.domain.user.exception.UserErrorCode;
import com.example.toget.domain.user.exception.UserException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JWT(JSON Web Token) 발급·검증기.
 *
 * [JWT 구조] "header.payload.signature" 형태의 문자열이며 각 파트는 Base64URL 인코딩된다.
 *  - header    : 서명 알고리즘 정보 { "alg": "HS256", "typ": "JWT" }
 *  - payload   : 클레임(claim)이라 부르는 데이터 조각들 (sub=사용자 ID, exp=만료시각 등)
 *  - signature : "header.payload"를 비밀 키로 HMAC-SHA256 서명한 값
 *                → 서버만 아는 키로 서명하므로, payload를 위조하면 서명이 달라져 검증에서 걸린다.
 *
 * [토큰 2종 전략]
 *  - access token  : API 호출 때마다 Authorization 헤더로 전달. 수명이 짧다(기본 1시간).
 *  - refresh token : access token 만료 시 재발급 용도로만 사용. 수명이 길다(기본 14일).
 *    탈취 피해를 줄이기 위해 jti(토큰 고유 ID)를 DB에 저장해 두고 재사용을 감지한다(AuthService 참고).
 */
@Component // 스프링이 이 클래스의 객체를 하나 만들어 빈(Bean)으로 등록 → 다른 곳에서 주입받아 사용
public class JwtProvider {

    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON <-> 객체 변환기 (스레드 안전하므로 재사용)
    private final byte[] secret;
    private final long accessTokenValiditySeconds;
    private final long refreshTokenValiditySeconds;

    // @Value : application.yaml의 설정값을 생성자 파라미터로 주입 ("기본값" 문법은 ${키:기본값})
    public JwtProvider(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.access-token-validity-seconds:3600}") long accessTokenValiditySeconds,
                       @Value("${jwt.refresh-token-validity-seconds:1209600}") long refreshTokenValiditySeconds) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        // HS256은 해시 출력과 같은 32바이트(256비트) 이상의 키가 표준(RFC 7518) —
        // 짧은 키는 무차별 대입으로 서명 키가 통째로 털릴 수 있어 기동 자체를 막는다
        if (this.secret.length < 32) {
            throw new IllegalStateException("jwt.secret은 32바이트(256비트) 이상이어야 합니다.");
        }
        this.accessTokenValiditySeconds = accessTokenValiditySeconds;
        this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
    }

    public String createAccessToken(Long userId) {
        return createToken(userId, null, TOKEN_TYPE_ACCESS, accessTokenValiditySeconds);
    }

    /** refresh token에는 jti(tokenId)를 심어서 DB 저장값과 대조할 수 있게 한다 */
    public String createRefreshToken(Long userId, String tokenId) {
        return createToken(userId, tokenId, TOKEN_TYPE_REFRESH, refreshTokenValiditySeconds);
    }

    /**
     * 토큰 검증 + 클레임 추출. 검증 순서:
     *  1) 3파트 구조인지 → 2) 서명이 우리 키로 만든 게 맞는지 → 3) 토큰 타입(access/refresh)과 만료 확인
     * 어느 단계든 실패하면 일괄 401(UNAUTHORIZED)로 처리해 공격자에게 실패 이유를 노출하지 않는다.
     */
    public JwtClaims parse(String token, String expectedType) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new UserException(UserErrorCode.UNAUTHORIZED);
            }

            // 받은 header.payload를 우리 비밀 키로 다시 서명해 보고, 토큰에 붙어 온 서명과 비교
            // → 일치하면 "우리 서버가 발급했고 변조되지 않은" 토큰임이 보장된다
            String unsignedToken = parts[0] + "." + parts[1];
            String expectedSignature = sign(unsignedToken);
            // 일반 equals는 앞자리가 다르면 바로 끝나서 비교 시간으로 값을 유추(타이밍 공격)할 수 있어
            // 항상 같은 시간이 걸리는 상수 시간 비교를 사용한다
            if (!MessageDigestUtil.constantTimeEquals(expectedSignature, parts[2])) {
                throw new UserException(UserErrorCode.UNAUTHORIZED);
            }

            JsonNode payload = objectMapper.readTree(base64UrlDecode(parts[1]));
            String type = payload.path("typ").asText(null);
            long expiration = payload.path("exp").asLong(0);
            // refresh token을 access 자리에 꽂는 식의 "토큰 타입 바꿔치기"와 만료를 함께 차단
            if (!expectedType.equals(type) || expiration < Instant.now().getEpochSecond()) {
                throw new UserException(UserErrorCode.UNAUTHORIZED);
            }

            Long userId = payload.path("sub").asLong();
            String tokenId = payload.path("jti").asText(null);
            return new JwtClaims(userId, tokenId, type);
        } catch (UserException e) {
            throw e; // 위에서 의도적으로 던진 401은 그대로 전달
        } catch (Exception e) {
            // Base64 깨짐, JSON 파싱 실패 등 나머지 모든 오류도 401로 통일
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }
    }

    public Long getUserId(JwtClaims claims) {
        return claims.userId();
    }

    private String createToken(Long userId, String tokenId, String type, long validitySeconds) {
        try {
            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            long now = Instant.now().getEpochSecond(); // 초 단위 Unix time — JWT 표준(exp, iat)이 초 단위
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", userId);          // subject: 토큰의 주인(사용자 ID) — 표준 클레임
            payload.put("typ", type);            // 커스텀 클레임: access/refresh 구분용
            payload.put("iat", now);             // issued at: 발급 시각
            payload.put("exp", now + validitySeconds); // expiration: 만료 시각
            if (tokenId != null) {
                payload.put("jti", tokenId);     // JWT ID: refresh token 재사용 감지에 쓰는 고유 식별자
            }

            String unsignedToken = base64UrlEncode(objectMapper.writeValueAsBytes(header))
                    + "."
                    + base64UrlEncode(objectMapper.writeValueAsBytes(payload));
            return unsignedToken + "." + sign(unsignedToken);
        } catch (Exception e) {
            // 발급 실패는 클라이언트 잘못(401)이 아닌 서버 내부 오류 — 공통 핸들러가 500으로 응답
            throw new IllegalStateException("JWT 생성에 실패했습니다.", e);
        }
    }

    /** HMAC-SHA256 서명: "비밀 키 + 메시지"의 해시라서 키를 모르면 같은 서명을 만들 수 없다 */
    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return base64UrlEncode(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    // JWT는 URL-safe Base64(+ → -, / → _)에 패딩(=) 없는 형식을 쓴다
    private String base64UrlEncode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}
