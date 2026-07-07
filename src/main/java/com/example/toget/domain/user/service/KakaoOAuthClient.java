package com.example.toget.domain.user.service;

import com.example.toget.domain.user.dto.OAuthUserInfo;
import com.example.toget.domain.user.enums.OauthProvider;
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
 */
@Component
public class KakaoOAuthClient implements OAuthClient {

    private final RestClient restClient; // 스프링의 동기 HTTP 클라이언트 (RestTemplate의 후속)
    private final String userInfoUri;

    // 타임아웃이 설정된 공용 빈(RestClientConfig의 oauthRestClient)을 주입받는다
    public KakaoOAuthClient(RestClient oauthRestClient,
                            @Value("${oauth.kakao.user-info-uri}") String userInfoUri) {
        this.restClient = oauthRestClient;
        this.userInfoUri = userInfoUri;
    }

    /** AuthService가 List<OAuthClient>에서 담당 구현체를 고를 때 사용하는 식별자 */
    @Override
    public OauthProvider provider() {
        return OauthProvider.KAKAO;
    }

    @Override
    public OAuthUserInfo verify(String identityToken) {
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
        return new OAuthUserInfo(
                body.path("id").asText(),                      // 카카오 회원번호 → 우리 DB의 oauth_id
                account.path("email").asText(null),            // 동의 안 했으면 없을 수 있어 기본값 null
                profile.path("nickname").asText(null),
                profile.path("profile_image_url").asText(null)
        );
    }
}
