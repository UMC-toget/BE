package com.example.toget.global.config;

import com.example.toget.global.apiPayload.code.GeneralErrorCode;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@EnableWebSecurity // Spring Security 설정을 활성화, 직접 작성한 보안 설정이 Spring Security의 기본 설정보다 우선 적용
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityErrorResponseWriter errorResponseWriter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          SecurityErrorResponseWriter errorResponseWriter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.errorResponseWriter = errorResponseWriter;
    }

    // 인증 없이 모든 HTTP 메소드를 접근할 수 있는 Public API 경로 정의
    private final String[] allowAllUris = {
        // Swagger 허용
        "/swagger-ui/**",
        "/swagger-resources/**",
        "/v3/api-docs/**",

        // 카카오/구글 소셜 로그인 및 토큰 재발급 API 허용
        "/api/v1/auth/tokens/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
        // REST API 서버이므로 CSRF 보호 비활성화 (Stateless 방식)
        .csrf(AbstractHttpConfigurer::disable)
        
        // CORS 설정 적용
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))

        // REST API 서버이므로 Session을 생성하지 않고 Stateless 상태 유지
        .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )

        // 소셜 로그인 기반의 stateless API 서버이므로 폼 로그인 및 기본 HTTP 로그인 비활성화
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)

        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

        // 시큐리티 단계에서 거부된 요청도 ApiResponse 포맷으로 응답 (기본값은 빈 본문 403)
        .exceptionHandling(handling -> handling
                .authenticationEntryPoint((request, response, e) ->
                        errorResponseWriter.write(response, GeneralErrorCode.UNAUTHORIZED))
                .accessDeniedHandler((request, response, e) ->
                        errorResponseWriter.write(response, GeneralErrorCode.FORBIDDEN)))

        // HTTP 요청에 대한 접근 제어 설정
        .authorizeHttpRequests(requests -> requests

                // 로그아웃은 /auth/tokens/** permitAll 패턴에 걸리지만 인증이 필요하므로 먼저 예외 처리
                .requestMatchers(HttpMethod.DELETE, "/api/v1/auth/tokens/me").authenticated()

                // 회원가입 완료 — 아직 회원이 아닌 사람이 호출하므로 인증을 요구할 수 없다.
                // 신원 확인은 요청 본문의 서명된 가입 토큰(signup token)으로 수행한다.
                // POST 하나만 열고 /users/me 등 나머지는 아래 anyRequest().authenticated()에 그대로 걸린다. (issue #61)
                .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()

                // S3 Presigned URL 발급 — 회원가입 시 프로필 사진 업로드 등 비회원 상태에서도 사용 가능하도록 허용
                .requestMatchers(HttpMethod.POST, "/api/v1/images/upload-requests").permitAll()

                // 웹 사진 검색 프록시 — 회원가입 중 프로필 설정, 게스트 기여 플로우에서도 사진을 골라야 하므로 허용.
                // /api/v1/images/** 같은 와일드카드가 아니라 GET + 정확한 경로만 열어
                // 이 도메인에 이후 추가될 API가 자동으로 개방되지 않게 한다. (issue #102)
                .requestMatchers(HttpMethod.GET, "/api/v1/images/search").permitAll()

                // 이미지 가져오기(외부 URL → S3 재업로드) — 검색 결과 선택 직후 호출되므로
                // 위 검색 API와 같은 인증 범위를 가져야 한다. 정확한 경로 + POST만 개방.
                // 서버가 사용자가 준 URL로 요청을 보내는 구조라 SSRF 방어는
                // ExternalImageImportService에서 수행한다. (issue #102)
                .requestMatchers(HttpMethod.POST, "/api/v1/images/imports").permitAll()

                // Swagger 및 소셜 로그인 API 무조건 허용
                .requestMatchers(allowAllUris).permitAll()

                // 비회원(게스트)도 참여/조회할 수 있는 공유용 API 명세 반영
                // ant 패턴의 *는 경로 세그먼트 한 칸만 매칭하므로 목록(.../contributions)과
                // 상세(.../contributions/{contributionId})는 각각 따로 열어줘야 한다.
                // 상세 조회는 GET만 허용 — 같은 경로의 PATCH(기여 금액 수정)는 개설자 전용이라
                // 아래 anyRequest().authenticated()에 그대로 걸린다.
                .requestMatchers(HttpMethod.GET, "/api/v1/shared-fundings/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/fundings/*/contributions").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/fundings/*/contributions/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/fundings/*/contributions").permitAll()

                // 공통 리소스 정보 조회는 인증 없이 가능하도록 설정.
                // contribution-backgrounds / characters / invitation-backgrounds / banks의 POST·PUT·PATCH·DELETE는
                // 아래 anyRequest().authenticated()로 로그인을 요구하고, 그 위에
                // @AdminOnly + AdminOnlyInterceptor가 "관리자 계정인가"를 한 겹 더 검사한다.
                // (JWT에 role 클레임이 없어 hasRole("ADMIN")을 쓸 수 없으므로 인터셉터 방식을 택했다.
                //  관리자가 여러 명/등급으로 확장되면 role 컬럼 + GrantedAuthority 매핑으로 교체 예정)
                .requestMatchers(HttpMethod.GET, "/api/v1/products", "/api/v1/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/characters/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/invitation-backgrounds", "/api/v1/invitation-backgrounds/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/contribution-backgrounds", "/api/v1/contribution-backgrounds/**").permitAll()
                // 은행 목록 — 계좌 등록 화면의 은행 선택지. HttpMethod.GET을 반드시 명시할 것:
                // 생략하면 같은 경로의 PATCH(관리자 전용 아이콘 교체)까지 열린다.
                .requestMatchers(HttpMethod.GET, "/api/v1/banks", "/api/v1/banks/**").permitAll()
                // 은행 추론 — 계좌 등록 전 입력된 계좌번호로 은행을 추론하는 API.
                .requestMatchers(HttpMethod.POST, "/api/v1/banks/detections").permitAll()

                // 후기/소식/마음전하기 조회 및 초대장 조회는 개설자 검증 없이 링크를 아는 누구나 조회 가능
                .requestMatchers(HttpMethod.GET, "/api/v1/fundings/*/reviews/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/fundings/*/reviews/*/invitation").permitAll()
                // 초대장 카드 조회 — GET만 허용. 같은 경로의 PUT(초대장 수정)은 개설자 전용이므로
                // HttpMethod를 지정하지 않으면 수정 API까지 열리니 주의.
                .requestMatchers(HttpMethod.GET, "/api/v1/fundings/*/invitations").permitAll()
                // 정산 계좌 조회 — 비회원 참여자도 입금을 위해 계좌 정보를 알아야 하므로 개설자 검증 없이
                // 개방한다. GET만 허용 — 같은 경로의 PATCH(계좌 변경)는 개설자 전용이라 그대로 인증이 필요하다.
                .requestMatchers(HttpMethod.GET, "/api/v1/fundings/*/account").permitAll()
                // TOGETHER_GIFT 상세 조회 — 개설자/공동관리자/일반참여자/비회원 모두 조회만 가능하고
                // (액션 버튼 활성화 여부는 응답의 myRole로 프론트가 판단), 액션 API들은 이 매처에 걸리지
                // 않으므로 계속 인증·역할 검증을 받는다. (issue #101)
                .requestMatchers(HttpMethod.GET, "/api/v1/fundings/*/dashboards/together-gift").permitAll()
                // 그 외의 API 및 리소스 요청은 로그인 필요
                .anyRequest().authenticated()
        );

        return http.build();
    }

    // JwtAuthenticationFilter가 @Component라서 부트가 서블릿 컨테이너에도 자동 등록해 버리는데,
    // 그러면 시큐리티 체인 밖에서 한 번 더 매핑된다. 등록을 꺼서 시큐리티 체인 안에서만 실행되게 한다.
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    // CORS 설정을 처리하는 Bean 정의
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 개발 편의를 위해 모든 출처를 허용하나, 실제 운영 시 허용할 도메인(클라이언트 주소)으로 구체화해야 함
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Cache-Control"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // 비밀번호 암호화 처리를 위한 PasswordEncoder Bean 유지 (회원 가입/관리 등에서 사용될 수 있음)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
