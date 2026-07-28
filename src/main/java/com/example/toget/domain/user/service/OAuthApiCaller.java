package com.example.toget.domain.user.service;

import com.example.toget.domain.user.exception.UserErrorCode;
import com.example.toget.domain.user.exception.UserException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import tools.jackson.databind.JsonNode;

import java.util.function.Supplier;

/**
 * 소셜 공급자(카카오/구글) API 호출의 실패를 에러코드로 분류하는 공용 헬퍼.
 *
 * <p>[왜 필요한가] 이전에는 모든 예외를 catch해서 일괄 401로 바꿨다. 그러면 구글이 5xx를 주거나
 * 타임아웃이 났을 때도 클라이언트는 "토큰이 무효하다"는 응답을 받게 되고, 프론트는 이를 만료로
 * 오해해 멀쩡한 사용자를 로그아웃시킨다. 게다가 issue #65로 구글 호출이 1회 → 2회로 늘면서
 * 이런 일시적 실패를 만날 확률도 두 배가 됐다.
 *
 * <p>[분류 기준]
 * <ul>
 *   <li>4xx — 공급자가 "이 토큰은 무효하다"고 판정한 것 → 401 (단, 429는 아래 참고)</li>
 *   <li>429 — 우리 서버의 호출량 문제이지 토큰 문제가 아니다 → 502</li>
 *   <li>5xx·타임아웃·DNS 실패·응답 파싱 실패 — 토큰의 유효 여부를 판단할 수 없다 → 502</li>
 * </ul>
 * 판단할 수 없을 때 401이 아닌 502를 주는 것이 핵심이다. 검증에 성공하지 못한 요청을
 * 통과시키는 경우는 없으므로 fail-closed 원칙은 그대로 유지된다.
 */
final class OAuthApiCaller {

    private OAuthApiCaller() {
    }

    /**                                                                    // ← 추가 (메서드 전체)
     * 토큰이 비어 있으면 외부 호출 없이 즉시 401로 거부한다.
     *
     * <p>컨트롤러의 @NotBlank가 먼저 걸러주지만, 서비스 계층이 단독으로 호출될 수 있으므로
     * 여기서도 방어한다. 이 검사가 없으면 null 토큰이 공급자마다 제각각 실패한다 —
     * 구글은 토큰 형태 판별(String.split)에서 NPE가 나 500이 되고,
     * 카카오는 "Bearer null"로 무의미한 외부 호출을 한 번 한 뒤에야 401이 된다.
     */
    static void requireToken(String token) {
        if (token == null || token.isBlank()) {
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }
    }

    /**
     * 공급자 API를 호출하고, 실패를 위 기준대로 UserException으로 변환한다.
     * 응답 본문이 비어 있으면(null) 정상 응답으로 볼 수 없으므로 502로 처리한다.
     */
    static JsonNode getJson(Supplier<JsonNode> request) {
        JsonNode body;
        try {
            body = request.get();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                throw new UserException(UserErrorCode.OAUTH_PROVIDER_UNAVAILABLE);
            }
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        } catch (RuntimeException e) {
            // HttpServerErrorException(5xx), ResourceAccessException(타임아웃/네트워크),
            // 본문 변환 실패 등 — 토큰이 유효한지 알 수 없는 모든 경우
            throw new UserException(UserErrorCode.OAUTH_PROVIDER_UNAVAILABLE);
        }
        // 본문 검사를 필드 추출보다 먼저 — 순서가 뒤바뀌면 응답이 없을 때 NPE가 나 500이 된다
        if (body == null) {
            throw new UserException(UserErrorCode.OAUTH_PROVIDER_UNAVAILABLE);
        }
        return body;
    }

    /**
     * JSON 필드를 문자열로 읽되, 노드나 필드가 없거나 null이면 null을 반환한다.
     *
     * <p>path()는 필드가 없을 때 MissingNode를 반환하는데, Jackson 3부터는 그 위에
     * textValue()/stringValue()를 호출하면 JsonNodeException을 던진다(Jackson 2는 null 반환).
     * 반면 get()은 필드가 없으면 null을 주므로 누락과 명시적 null을 함께 걸러낼 수 있다.
     * 카카오·구글 모두 동의하지 않았거나 scope에 없는 항목을 응답에서 아예 생략하므로
     * 이 경로를 반드시 방어해야 한다.
     *
     * <p>node 자체가 null인 경우는 현재 호출부에는 없지만(getJson이 non-null을 보장하고
     * path()는 MissingNode를 준다), 공용 헬퍼라 호출부가 늘어날 수 있어 함께 방어한다.
     */
    static String textOrNull(JsonNode node, String field) {
        if (node == null) {                                                // ← 추가
            return null;                                                   // ← 추가
        }                                                                  // ← 추가
        JsonNode value = node.get(field);
        return (value == null || value.isNull()) ? null : value.asString();
    }
}
