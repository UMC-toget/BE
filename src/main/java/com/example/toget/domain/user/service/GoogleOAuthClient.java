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
        // 응답 자체가 비어 있을 수 있으므로 사용 전에 먼저 검사한다 (NPE → 500 방지)
        if (body == null) {
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }
        String sub = body.path("sub").textValue();
        if (sub == null) {
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }
        // aud(발급 대상 클라이언트 ID) 검증 — 없으면 타 서비스용으로 발급된 구글 토큰으로도 로그인 가능
        String aud = body.path("aud").textValue();
        if (clientId.isBlank() || !clientId.equals(aud)) {
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }
        return new OAuthUserInfo(
                sub, // 구글 계정 고유 ID → 우리 DB의 oAuthId
                body.path("email").textValue(),
                body.path("name").textValue(),
                body.path("picture").textValue()
        );
    }
}
