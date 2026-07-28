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
                // contribution-backgrounds / characters / invitation-backgrounds의 POST·PUT·DELETE는
                // 아래 anyRequest().authenticated()로 로그인을 요구하고, 그 위에
                // @AdminOnly + AdminOnlyInterceptor가 "관리자 계정인가"를 한 겹 더 검사한다.
                // (JWT에 role 클레임이 없어 hasRole("ADMIN")을 쓸 수 없으므로 인터셉터 방식을 택했다.
                //  관리자가 여러 명/등급으로 확장되면 role 컬럼 + GrantedAuthority 매핑으로 교체 예정)
                .requestMatchers(HttpMethod.GET, "/api/v1/products", "/api/v1/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/characters/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/invitation-backgrounds", "/api/v1/invitation-backgrounds/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/contribution-backgrounds", "/api/v1/contribution-backgrounds/**").permitAll()

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
