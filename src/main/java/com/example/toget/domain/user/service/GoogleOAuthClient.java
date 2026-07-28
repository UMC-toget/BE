package com.example.toget.domain.user.service;

import com.example.toget.domain.user.dto.OAuthUserInfo;
import com.example.toget.domain.user.enums.OAuthProvider;
import com.example.toget.domain.user.exception.UserErrorCode;
import com.example.toget.domain.user.exception.UserException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.example.toget.domain.user.service.OAuthApiCaller.getJson;
import static com.example.toget.domain.user.service.OAuthApiCaller.textOrNull;

/**
 * 구글 로그인 토큰 검증기 — access token 방식 (issue #65).
 *
 * [왜 ID token이 아니라 access token인가]
 * 프론트는 피그마 디자인 버튼 클릭 시 구글 팝업(google.accounts.oauth2.initTokenClient)을 띄우는
 * token model을 쓴다. 서드파티 쿠키가 차단된 환경에서 FedCM으로 전환된 구글 버튼이
 * 투명(opacity: 0) 오버레이 클릭을 클릭재킹으로 차단하기 때문이다.
 * 팝업(token model)은 ID token이 아니라 access token을 반환하므로 검증 방식이 달라진다.
 *
 * [검증 절차 — 카카오(KakaoOAuthClient)와 같은 2회 호출 구조]
 *  1. 토큰 정보 API(tokeninfo?access_token=)의 aud가 우리 client_id와 일치하는지 확인
 *     → 타 서비스용으로 발급된 구글 토큰으로 로그인하는 것을 막는다.
 *     userinfo는 누구의 토큰이든 소유자 정보를 돌려주므로 aud 검증 없이는 안전하지 않다.
 *  2. 사용자 정보 API(userinfo)를 Bearer 헤더로 호출해 sub/email/name/picture를 얻는다.
 *     무효한 토큰이면 구글이 401을 주므로 이 호출 자체가 유효성 검증을 겸한다.
 *
 * [전환 기간 하위호환]
 * 프론트와 백엔드 배포는 동시에 일어나지 않으므로, 어느 한쪽만 먼저 나가면 그 사이 구글 로그인이
 * 전부 401이 된다. 이를 피하려고 토큰 형태를 보고 구/신 검증 경로를 모두 지원한다 — verifyLegacyIdToken 참고.
 *
 * [실패 분류] 구글 장애·타임아웃은 401이 아니라 502로 나간다 — OAuthApiCaller 참고.
 */
@Component
public class GoogleOAuthClient implements OAuthClient {

    private final RestClient restClient;
    private final String tokenInfoUri;
    private final String userInfoUri;
    private final String clientId;

    // 타임아웃이 설정된 공용 빈(RestClientConfig의 oauthRestClient)을 주입받는다
    public GoogleOAuthClient(RestClient oauthRestClient,
                             @Value("${oauth.google.token-info-uri}") String tokenInfoUri,
                             @Value("${oauth.google.user-info-uri}") String userInfoUri,
                             @Value("${oauth.google.client-id:}") String clientId) {
        this.restClient = oauthRestClient;
        this.tokenInfoUri = tokenInfoUri;
        this.userInfoUri = userInfoUri;
        this.clientId = clientId;
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public OAuthUserInfo verify(String identityToken) {
        // client-id가 없으면 어떤 응답이 와도 aud를 대조할 수 없다 → 외부 호출 전에 즉시 거부(fail-closed).
        // 설정 누락 상태에서 구글 API를 두드리는 것 자체가 낭비이므로 검사를 맨 앞에 둔다.
        if (clientId.isBlank()) {
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }
        // TODO(issue #65): 프론트가 팝업(token model)으로 완전히 전환되고 구버전 클라이언트가
        //  더 이상 없다고 확인되면 이 분기와 verifyLegacyIdToken()을 제거할 것.
        if (looksLikeIdToken(identityToken)) {
            return verifyLegacyIdToken(identityToken);
        }

        verifyAudience(identityToken); // 우리 앱에서 발급된 토큰인지 먼저 확인

        JsonNode body = fetchUserInfo(identityToken);
        String sub = textOrNull(body, "sub");
        if (sub == null) {
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }
        // email/name/picture는 프론트가 요청한 scope에 따라 응답에서 빠질 수 있다.
        // 필수값이 아니므로 없으면 null로 둔다.
        return new OAuthUserInfo(
                sub, // 구글 계정 고유 ID → 우리 DB의 oAuthId
                textOrNull(body, "email"),
                textOrNull(body, "name"),
                textOrNull(body, "picture")
        );
    }

    /**
     * 토큰이 ID token(JWT)인지 판별한다.
     *
     * <p>구글 access token은 불투명 문자열(ya29.…)이라 형식 보장이 없지만, ID token은 반드시
     * "header.payload.signature" 3조각의 JWT이고 헤더가 base64url JSON이다. 그래서 조각 수만 세지 않고
     * 헤더를 실제로 디코드해 '{'로 시작하는지까지 확인한다 — access token에 점이 두 개 들어 있어도
     * 오탐하지 않기 위해서다. (판별에 실패하면 access token 경로로 가고, 그 경로가 다시 검증하므로
     * 이 메서드가 틀려도 인증이 느슨해지지는 않는다.)
     */
    private static boolean looksLikeIdToken(String token) {
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            return false;
        }
        try {
            String header = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            return header.startsWith("{");
        } catch (IllegalArgumentException e) { // base64url이 아니면 ID token이 아니다
            return false;
        }
    }

    /**
     * [레거시 — issue #65 전환 기간 한정] ID token 검증 (GET /tokeninfo?id_token=...).
     *
     * <p>구글이 서명·만료를 확인해 주고 payload(sub, email, aud 등)를 그대로 돌려주므로
     * userinfo를 따로 부를 필요가 없다. aud 검증 책임은 access token 경로와 동일하게 우리에게 있다.
     * 아직 배포되지 않은 구버전 프론트를 위한 경로이며, 전환 완료 후 제거 대상이다.
     */
    private OAuthUserInfo verifyLegacyIdToken(String idToken) {
        JsonNode body = getJson(() -> restClient.get()
                .uri(tokenInfoUri + "?id_token={token}", idToken)
                .retrieve()
                .body(JsonNode.class));
        String sub = textOrNull(body, "sub");
        if (sub == null) {
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }
        requireOurAudience(textOrNull(body, "aud"));
        return new OAuthUserInfo(
                sub,
                textOrNull(body, "email"),
                textOrNull(body, "name"),
                textOrNull(body, "picture")
        );
    }

    /**
     * 발급 대상 검증 — 토큰 정보 API의 aud가 설정된 우리 client_id와 일치하지 않으면 거부.
     */
    private void verifyAudience(String identityToken) {
        JsonNode body = getJson(() -> restClient.get()
                // {token} 자리에 값이 URL 인코딩되어 안전하게 치환된다 (문자열 직접 연결보다 안전)
                .uri(tokenInfoUri + "?access_token={token}", identityToken)
                .retrieve()
                .body(JsonNode.class));
        requireOurAudience(textOrNull(body, "aud"));
    }

    /**
     * aud가 우리 client_id와 다르면 거부 (fail-closed).
     * aud 필드 자체가 없어도 null != clientId이므로 거부된다.
     */
    private void requireOurAudience(String aud) {
        if (!clientId.equals(aud)) {
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }
    }

    /** 사용자 정보 API 호출 — 무효 토큰이면 구글이 401로 응답하고 retrieve()가 예외를 던진다 */
    private JsonNode fetchUserInfo(String identityToken) {
        return getJson(() -> restClient.get()
                .uri(userInfoUri)
                .header("Authorization", "Bearer " + identityToken)
                .retrieve()
                .body(JsonNode.class));
    }
}
