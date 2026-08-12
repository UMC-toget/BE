package com.example.toget.domain.funding.controller;

import com.example.toget.domain.funding.dto.response.FundingTogetherGiftDashboardResponse;
import com.example.toget.domain.funding.service.FundingQueryService;
import com.example.toget.domain.funding.service.FundingService;
import com.example.toget.domain.gift.service.FundingGiftService;
import com.example.toget.domain.user.service.ActiveUserReader;
import com.example.toget.domain.user.service.JwtProvider;
import com.example.toget.global.config.AdminProperties;
import com.example.toget.global.config.SecurityConfig;
import com.example.toget.global.config.SecurityErrorResponseWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.autoconfigure.web.DataWebAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SecurityConfig가 TOGETHER_GIFT 대시보드 조회를 실제로 permitAll 처리하는지 검증하는 웹 슬라이스 테스트 (issue #101).
 *
 * [설계 포인트]
 *  - FundingQueryServiceTest(Mockito 단위 테스트)는 서비스 로직만 검증할 뿐, "Authorization 헤더
 *    없이 이 경로를 쳤을 때 실제 SecurityFilterChain이 401을 내지 않는지"는 검증하지 못한다.
 *    permitAll 매처의 ant 패턴(HttpMethod, 경로 세그먼트 수)을 잘못 쓰면 컴파일은 되지만 런타임에
 *    조용히 401을 내므로, 실제 SecurityConfig를 태워봐야만 잡을 수 있다.
 *  - @WebMvcTest(FundingController.class)에 SecurityConfig를 @Import해 실제 authorizeHttpRequests
 *    규칙을 그대로 적용한다. JwtProvider는 이 시나리오(토큰 없음)에서는 호출되지 않으므로 목으로
 *    대체해 JWT 시크릿 같은 실제 설정 없이도 돌아가게 한다. SecurityErrorResponseWriter는 401
 *    응답의 실제 status를 쓰는 주체라 목으로 대체하면 안 되므로(아래 주석 참고) 실 구현을 쓴다.
 *  - 같은 컨트롤러의 인증 필요 엔드포인트(POST /fundings)가 여전히 401을 내는지도 함께 확인해,
 *    "시큐리티 설정이 통째로 빠져서 우연히 통과한 것"이 아님을 보장한다.
 */
// DataWebAutoConfiguration(Pageable 웹 바인딩)은 이 컨트롤러의 테스트 대상 두 엔드포인트와 무관하고,
// 이 앱에 JPA EntityManagerFactory가 없는 슬라이스 컨텍스트에서 jpaMappingContext 빈 생성에
// 실패시키므로 제외한다.
@WebMvcTest(controllers = FundingController.class, excludeAutoConfiguration = DataWebAutoConfiguration.class)
@Import({SecurityConfig.class, SecurityErrorResponseWriter.class})
class FundingControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FundingQueryService fundingQueryService;

    @MockitoBean
    private FundingService fundingService;

    @MockitoBean
    private FundingGiftService fundingGiftService;

    // SecurityConfig/JwtAuthenticationFilter를 실제로 구동하기 위한 의존 빈.
    // 이 테스트는 "토큰 없는 요청"만 다루므로 실제 파싱 로직까지는 필요 없어 목으로 대체한다.
    @MockitoBean
    private JwtProvider jwtProvider;

    // SecurityErrorResponseWriter는 목으로 만들면 안 된다 — 401 응답의 실제 status를 쓰는 주체라
    // 목의 write()는 no-op이라 두 번째 테스트가 "거짓 200"으로 통과해 버린다.
    // 의존성이 없는 단순 클래스라 실 구현을 그대로 @Import해서 쓴다.

    // AdminOnlyInterceptor(HandlerInterceptor)가 웹 슬라이스에 함께 딸려 들어오며 요구하는 설정 빈.
    // 이 테스트가 다루는 두 엔드포인트는 관리자 전용이 아니라 값 자체는 의미가 없다.
    @MockitoBean
    private AdminProperties adminProperties;

    @MockitoBean
    private ActiveUserReader activeUserReader;

    @Test
    @DisplayName("Authorization 헤더 없이 TOGETHER_GIFT 대시보드를 조회해도 401이 아니다")
    void togetherGiftDashboardIsOpenWithoutLogin() throws Exception {
        given(fundingQueryService.getTogetherGiftDashboard(null, 1L)).willReturn(dummyResponse());

        mockMvc.perform(get("/api/v1/fundings/1/dashboards/together-gift"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("같은 컨트롤러의 인증 필요 엔드포인트는 토큰 없이 호출하면 여전히 401이다 — 시큐리티가 꺼진 게 아님을 확인")
    void protectedEndpointStillRequiresAuth() throws Exception {
        mockMvc.perform(post("/api/v1/fundings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private FundingTogetherGiftDashboardResponse dummyResponse() {
        return new FundingTogetherGiftDashboardResponse(
                1L, "펀딩 제목", "SELECTING", null, null, null, null, null, null, null,
                List.of(), List.of(), null, null, null, null
        );
    }
}
