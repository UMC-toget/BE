package com.example.toget.domain.user.service;

import com.example.toget.domain.user.dto.OAuthUserInfo;
import com.example.toget.domain.user.exception.UserErrorCode;
import com.example.toget.domain.user.exception.UserException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

/**
 * GoogleOAuthClient 단위 테스트 (issue #65).
 *
 * access token 검증 절차 — 1) tokeninfo의 aud 검증 2) userinfo로 사용자 정보 조회 — 를
 * MockRestServiceServer로 외부 구글 API를 흉내 내어 검증한다.
 *
 * 핵심 계약:
 *  - aud가 우리 client_id와 일치하지 않으면 userinfo 호출 없이 즉시 거부 (타 앱용 토큰 차단)
 *  - client-id 미설정 시 외부 호출조차 하지 않고 거부 (fail-closed)
 *  - email/name/picture는 scope에 따라 빠질 수 있으므로 null을 허용하되 sub는 필수
 *  - 토큰이 무효하면 401, 구글이 응답하지 못하면 502 — 둘을 섞지 않는다
 *  - 전환 기간 동안 구버전 프론트의 ID token(JWT)도 기존 방식으로 계속 검증된다
 */
class GoogleOAuthClientTest {

    private static final String TOKEN_INFO_URI = "https://oauth2.googleapis.com/tokeninfo";
    private static final String USER_INFO_URI = "https://openidconnect.googleapis.com/v1/userinfo";
    private static final String CLIENT_ID = "test-client-id.apps.googleusercontent.com";
    private static final String ACCESS_TOKEN = "google-access-token";
    /** 구버전 프론트가 보내던 ID token — 헤더가 base64url JSON인 3조각 JWT ({"alg":"RS256"}.{}.sig) */
    private static final String ID_TOKEN = "eyJhbGciOiJSUzI1NiJ9.e30.signature";
    /** 점이 두 개 들어간 구글 access token — JWT로 오탐하면 안 된다 */
    private static final String DOTTED_ACCESS_TOKEN = "ya29.a0AfH6SMB.xyz";

    private MockRestServiceServer server;
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
    }

    private GoogleOAuthClient client(String clientId) {
        return new GoogleOAuthClient(restClient, TOKEN_INFO_URI, USER_INFO_URI, clientId);
    }

    /** tokeninfo가 주어진 aud로 응답하도록 세팅 */
    private void expectTokenInfo(String aud) {
        expectTokenInfoBody("{\"aud\":\"" + aud + "\"}");
    }

    private void expectTokenInfoBody(String json) {
        server.expect(requestTo(TOKEN_INFO_URI + "?access_token=" + ACCESS_TOKEN))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    /** userinfo가 주어진 JSON을 응답하도록 세팅 — Bearer 헤더로 호출되는지도 함께 검증 */
    private void expectUserInfo(String json) {
        server.expect(requestTo(USER_INFO_URI))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    /** 거부 사유(에러코드)까지 단정한다 — 401과 502를 섞지 않는 것이 이 클래스의 핵심 계약이라서다 */
    private static void assertRejectedWith(UserErrorCode expected, ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(UserException.class)
                .satisfies(e -> assertThat(((UserException) e).getCode()).isEqualTo(expected));
    }

    @Test
    @DisplayName("aud가 일치하면 userinfo에서 sub/email/name/picture를 추출한다")
    void verifiesAccessTokenAndExtractsUserInfo() {
        expectTokenInfo(CLIENT_ID);
        expectUserInfo("{\"sub\":\"1234567890\",\"email\":\"user@example.com\","
                + "\"name\":\"홍길동\",\"picture\":\"https://example.com/p.jpg\"}");

        OAuthUserInfo info = client(CLIENT_ID).verify(ACCESS_TOKEN);

        assertThat(info.oAuthId()).isEqualTo("1234567890");
        assertThat(info.email()).isEqualTo("user@example.com");
        assertThat(info.name()).isEqualTo("홍길동");
        assertThat(info.profileImageUrl()).isEqualTo("https://example.com/p.jpg");
        server.verify(); // 세팅한 두 요청이 모두 실제로 나갔는지 확인
    }

    @Test
    @DisplayName("aud가 일치하지 않으면 userinfo 호출 없이 401로 거부한다 — 타 앱용 토큰 차단")
    void rejectsTokenIssuedForAnotherApp() {
        expectTokenInfo("other-app-client-id");

        assertRejectedWith(UserErrorCode.UNAUTHORIZED, () -> client(CLIENT_ID).verify(ACCESS_TOKEN));
        server.verify(); // userinfo expectation이 없으므로, 호출됐다면 여기서 실패함
    }

    @Test
    @DisplayName("tokeninfo 응답에 aud 필드 자체가 없어도 거부한다")
    void rejectsWhenAudienceFieldIsMissing() {
        expectTokenInfoBody("{\"scope\":\"openid email\"}");

        assertRejectedWith(UserErrorCode.UNAUTHORIZED, () -> client(CLIENT_ID).verify(ACCESS_TOKEN));
        server.verify();
    }

    @Test
    @DisplayName("client-id가 미설정이면 구글을 호출하지도 않고 거부한다 — fail-closed")
    void rejectsWhenClientIdIsNotConfigured() {
        assertRejectedWith(UserErrorCode.UNAUTHORIZED, () -> client("").verify(ACCESS_TOKEN));
        server.verify(); // expectation을 하나도 걸지 않았으므로, 호출이 나갔다면 여기서 실패함
    }
    
    @ParameterizedTest(name = "토큰이 [{0}]이면 거부")
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("토큰이 비어 있으면 구글을 호출하지 않고 401로 거부한다")
    void rejectsBlankToken(String token) {
        assertRejectedWith(UserErrorCode.UNAUTHORIZED, () -> client(CLIENT_ID).verify(token));
        server.verify(); // expectation을 걸지 않았으므로, 외부 호출이 나갔다면 여기서 실패함
    }
    

    @Test
    @DisplayName("scope에 없는 항목이 응답에서 빠져도 sub만 있으면 null로 받아들인다")
    void toleratesMissingOptionalFields() {
        expectTokenInfo(CLIENT_ID);
        expectUserInfo("{\"sub\":\"1234567890\"}"); // email/name/picture 없음 (scope: openid만 요청한 경우)

        OAuthUserInfo info = client(CLIENT_ID).verify(ACCESS_TOKEN);

        assertThat(info.oAuthId()).isEqualTo("1234567890");
        assertThat(info.email()).isNull();
        assertThat(info.name()).isNull();
        assertThat(info.profileImageUrl()).isNull();
        server.verify();
    }

    @Test
    @DisplayName("userinfo 응답에 sub가 없으면 거부한다 — 사용자 식별자는 필수")
    void rejectsWhenSubIsMissing() {
        expectTokenInfo(CLIENT_ID);
        expectUserInfo("{\"email\":\"user@example.com\"}");

        assertRejectedWith(UserErrorCode.UNAUTHORIZED, () -> client(CLIENT_ID).verify(ACCESS_TOKEN));
        server.verify();
    }

    @Test
    @DisplayName("토큰을 쿼리스트링에 넣을 때 URL 인코딩한다 — 문자열 직접 연결이 아님을 보장")
    void encodesTokenInQueryString() {
        String awkwardToken = "token with space";
        server.expect(requestTo(containsString("access_token=token%20with%20space")))
                .andRespond(withSuccess("{\"aud\":\"" + CLIENT_ID + "\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(USER_INFO_URI))
                .andRespond(withSuccess("{\"sub\":\"1234567890\"}", MediaType.APPLICATION_JSON));

        assertThat(client(CLIENT_ID).verify(awkwardToken).oAuthId()).isEqualTo("1234567890");
        server.verify();
    }

    // --- 실패 분류: 무효 토큰(401) vs 공급자 장애(502) ---

    @Test
    @DisplayName("구글이 토큰을 무효로 거부(401)하면 401로 변환한다")
    void rejectsInvalidToken() {
        server.expect(requestTo(TOKEN_INFO_URI + "?access_token=" + ACCESS_TOKEN))
                .andRespond(withUnauthorizedRequest());

        assertRejectedWith(UserErrorCode.UNAUTHORIZED, () -> client(CLIENT_ID).verify(ACCESS_TOKEN));
        server.verify();
    }

    @Test
    @DisplayName("aud는 통과했지만 userinfo가 토큰을 거부하면 401로 변환한다 — revoke된 토큰")
    void rejectsTokenRevokedAfterAudienceCheck() {
        expectTokenInfo(CLIENT_ID);
        server.expect(requestTo(USER_INFO_URI))
                .andRespond(withUnauthorizedRequest());

        assertRejectedWith(UserErrorCode.UNAUTHORIZED, () -> client(CLIENT_ID).verify(ACCESS_TOKEN));
        server.verify();
    }

    @Test
    @DisplayName("구글이 5xx를 주면 401이 아니라 502로 변환한다 — 사용자를 로그아웃시키지 않기 위해")
    void translatesProviderOutageToBadGateway() {
        server.expect(requestTo(TOKEN_INFO_URI + "?access_token=" + ACCESS_TOKEN))
                .andRespond(withServerError());

        assertRejectedWith(UserErrorCode.OAUTH_PROVIDER_UNAVAILABLE,
                () -> client(CLIENT_ID).verify(ACCESS_TOKEN));
        server.verify();
    }

    @Test
    @DisplayName("구글 호출이 타임아웃되면 502로 변환한다")
    void translatesTimeoutToBadGateway() {
        server.expect(requestTo(TOKEN_INFO_URI + "?access_token=" + ACCESS_TOKEN))
                .andRespond(withException(new SocketTimeoutException("read timed out")));

        assertRejectedWith(UserErrorCode.OAUTH_PROVIDER_UNAVAILABLE,
                () -> client(CLIENT_ID).verify(ACCESS_TOKEN));
        server.verify();
    }

    // --- 전환 기간 하위호환 (issue #65) — 프론트 배포 완료 후 아래 3개 테스트와 구현을 함께 제거할 것 ---

    @Test
    @DisplayName("구버전 프론트의 ID token은 tokeninfo?id_token= 경로로 계속 검증된다 — 배포 시차 중 로그인 유지")
    void acceptsLegacyIdTokenDuringTransition() {
        server.expect(requestTo(TOKEN_INFO_URI + "?id_token=" + ID_TOKEN))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"aud\":\"" + CLIENT_ID + "\",\"sub\":\"1234567890\","
                        + "\"email\":\"user@example.com\",\"name\":\"홍길동\","
                        + "\"picture\":\"https://example.com/p.jpg\"}", MediaType.APPLICATION_JSON));

        OAuthUserInfo info = client(CLIENT_ID).verify(ID_TOKEN);

        // access token 경로와 동일한 sub가 나와야 기존 회원의 oAuthId가 유지된다
        assertThat(info.oAuthId()).isEqualTo("1234567890");
        assertThat(info.email()).isEqualTo("user@example.com");
        server.verify(); // userinfo expectation이 없으므로, 호출됐다면 여기서 실패함
    }

    @Test
    @DisplayName("ID token 경로에서도 aud가 다르면 거부한다 — 하위호환이 검증을 느슨하게 만들지 않는다")
    void rejectsLegacyIdTokenIssuedForAnotherApp() {
        server.expect(requestTo(TOKEN_INFO_URI + "?id_token=" + ID_TOKEN))
                .andRespond(withSuccess("{\"aud\":\"other-app-client-id\",\"sub\":\"1234567890\"}",
                        MediaType.APPLICATION_JSON));

        assertRejectedWith(UserErrorCode.UNAUTHORIZED, () -> client(CLIENT_ID).verify(ID_TOKEN));
        server.verify();
    }

    @Test
    @DisplayName("점이 포함된 access token을 ID token으로 오판하지 않는다")
    void doesNotMistakeDottedAccessTokenForIdToken() {
        server.expect(requestTo(TOKEN_INFO_URI + "?access_token=" + DOTTED_ACCESS_TOKEN))
                .andRespond(withSuccess("{\"aud\":\"" + CLIENT_ID + "\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(USER_INFO_URI))
                .andExpect(header("Authorization", "Bearer " + DOTTED_ACCESS_TOKEN))
                .andRespond(withSuccess("{\"sub\":\"1234567890\"}", MediaType.APPLICATION_JSON));

        assertThat(client(CLIENT_ID).verify(DOTTED_ACCESS_TOKEN).oAuthId()).isEqualTo("1234567890");
        server.verify();
    }
}
