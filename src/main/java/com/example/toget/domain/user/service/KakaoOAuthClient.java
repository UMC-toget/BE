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
 * 카카오 로그인 토큰 검증기.
 * 프론트가 카카오 SDK로 받은 access token을 카카오 사용자 정보 API(/v2/user/me)에 넣어 호출해 본다.
 * 카카오가 정상 응답하면 유효한 토큰이고, 응답 속의 id가 카카오가 보증하는 사용자 식별자다.
 *
 * 단, 카카오 access token은 "어느 카카오 앱에서든" 발급될 수 있으므로
 * 유효성 검사만으로는 부족하다 — 토큰 정보 API(/v1/user/access_token_info)의 app_id가
 * 우리 앱의 ID와 일치하는지도 확인해야 타 서비스용 토큰으로 로그인하는 것을 막을 수 있다.
 * (구글 쪽의 aud 검증과 같은 역할)
 */
@Component
public class KakaoOAuthClient implements OAuthClient {

    private final RestClient restClient; // 스프링의 동기 HTTP 클라이언트 (RestTemplate의 후속)
    private final String userInfoUri;
    private final String tokenInfoUri;
    private final String appId;

    // 타임아웃이 설정된 공용 빈(RestClientConfig의 oauthRestClient)을 주입받는다
    public KakaoOAuthClient(RestClient oauthRestClient,
                            @Value("${oauth.kakao.user-info-uri}") String userInfoUri,
                            @Value("${oauth.kakao.token-info-uri}") String tokenInfoUri,
                            @Value("${oauth.kakao.app-id:}") String appId) {
        this.restClient = oauthRestClient;
        this.userInfoUri = userInfoUri;
        this.tokenInfoUri = tokenInfoUri;
        this.appId = appId;
    }

    /** AuthService가 List<OAuthClient>에서 담당 구현체를 고를 때 사용하는 식별자 */
    @Override
    public OAuthProvider provider() {
        return OAuthProvider.KAKAO;
    }

    @Override
    public OAuthUserInfo verify(String identityToken) {
        verifyAppId(identityToken); // 우리 앱에서 발급된 토큰인지 먼저 확인

        JsonNode body;
        try {
            body = restClient.get()
                    .uri(userInfoUri)
                    .header("Authorization", "Bearer " + identityToken) // 카카오 API도 Bearer 방식
                    .retrieve() // 요청 실행 — 4xx/5xx 응답이면 예외 발생
                    .body(JsonNode.class);
        } catch (Exception e) {
            // 네트워크 오류든 카카오의 401이든, 클라이언트 입장에서는 "인증 실패" 하나로 취급
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }
        // path(): 키가 없어도 예외 대신 missing node를 반환 → null 체크 없이 안전하게 탐색 가능
        if (body == null || body.path("id").isMissingNode()) {
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }
        // 카카오 응답 구조: { id, kakao_account: { email, profile: { nickname, profile_image_url } } }
        JsonNode account = body.path("kakao_account");
        JsonNode profile = account.path("profile");

        String idString = body.path("id").isNumber() 
                ? String.valueOf(body.path("id").asLong()) 
                : body.path("id").textValue();

        return new OAuthUserInfo(
                idString,                                      // 카카오 회원번호 → 우리 DB의 oAuthId
                account.path("email").textValue(),            // 동의 안 했으면 없을 수 있어 기본값 null
                profile.path("nickname").textValue(),
                profile.path("profile_image_url").textValue()
        );
    }

    /**
     * 발급 앱 검증 — 토큰 정보 API의 app_id가 설정된 우리 앱 ID와 다르면 거부.
     * app-id 미설정(빈 값) 시에도 거부해, 검증 없이 열리는 일이 없도록 한다.
     */
    private void verifyAppId(String identityToken) {
        JsonNode body;
        try {
            body = restClient.get()
                    .uri(tokenInfoUri)
                    .header("Authorization", "Bearer " + identityToken)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }

        JsonNode appIdNode = body == null ? null : body.path("app_id");
        String tokenAppId = appIdNode == null || appIdNode.isMissingNode() ? null
                : appIdNode.isNumber() ? String.valueOf(appIdNode.asLong())
                : appIdNode.textValue();
        if (appId.isBlank() || !appId.equals(tokenAppId)) {
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }
    }
}
