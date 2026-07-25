package com.example.toget.domain.user.service;

import com.example.toget.domain.user.dto.OAuthUserInfo;
import com.example.toget.domain.user.enums.OAuthProvider;
import com.example.toget.domain.user.exception.UserErrorCode;
import com.example.toget.domain.user.exception.UserException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/**
 * 구글 로그인 토큰 검증기 (GET /tokeninfo?id_token=...).
 *
 * 카카오와 달리 구글은 "ID 토큰"(JWT)을 검증한다. tokeninfo 엔드포인트에 넘기면
 * 구글이 서명·만료를 확인해 주고 payload(sub, email, aud 등)를 돌려준다.
 * 단, "이 토큰이 우리 앱용으로 발급됐는지"(aud)는 호출한 쪽이 직접 확인해야 한다.
 */
@Component
public class GoogleOAuthClient implements OAuthClient {

    private final RestClient restClient;
    private final String tokenInfoUri;
    private final String clientId;

    public GoogleOAuthClient(RestClient oauthRestClient,
                             @Value("${oauth.google.token-info-uri}") String tokenInfoUri,
                             @Value("${oauth.google.client-id:}") String clientId) {
        this.restClient = oauthRestClient;
        this.tokenInfoUri = tokenInfoUri;
        this.clientId = clientId;
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public OAuthUserInfo verify(String identityToken) {
        JsonNode body;
        try {
            body = restClient.get()
                    // {token} 자리에 값이 URL 인코딩되어 안전하게 치환된다 (문자열 직접 연결보다 안전)
                    .uri(tokenInfoUri + "?id_token={token}", identityToken)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }
        // body 검사를 sub 추출보다 먼저 — 순서가 뒤바뀌면 응답이 없을 때 NPE가 나 401이 아닌 500이 된다
        if (body == null) {
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }
        String sub = textOrNull(body, "sub");
        if (sub == null) {
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }
        // aud(발급 대상 클라이언트 ID) 검증 — 없으면 타 서비스용으로 발급된 구글 토큰으로도 로그인 가능
        String aud = textOrNull(body, "aud");
        if (clientId.isBlank() || !clientId.equals(aud)) {
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }
        // email/name/picture는 요청 스코프에 따라 응답에서 아예 빠질 수 있다 (openid만 요청한 경우 등).
        // 필수값이 아니므로 없으면 null로 두고, 이후 프로필 갱신에서 채워지도록 한다.
        return new OAuthUserInfo(
                sub, // 구글 계정 고유 ID → 우리 DB의 oAuthId
                textOrNull(body, "email"),
                textOrNull(body, "name"),
                textOrNull(body, "picture")
        );
    }

    /**
     * JSON 필드를 문자열로 읽되, 없거나 null이면 null을 반환한다.
     *
     * <p>path()는 필드가 없을 때 MissingNode를 반환하는데, Jackson 3부터는 그 위에
     * textValue()/stringValue()를 호출하면 JsonNodeException을 던진다(Jackson 2는 null 반환).
     * 반면 get()은 필드가 없으면 null을 주므로 누락과 명시적 null을 함께 걸러낼 수 있다.
     */
    private static String textOrNull(JsonNode body, String field) {
        JsonNode node = body.get(field);
        return (node == null || node.isNull()) ? null : node.asString();
    }
}