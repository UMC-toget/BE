package com.example.toget.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

@Configuration
public class RestClientConfig {

    @Bean
    @ConditionalOnMissingBean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    // 외부 OAuth 서버(구글/카카오) 호출용 공용 RestClient
    // 타임아웃 미설정 시 외부 API 지연이 서블릿 스레드 고갈로 이어지므로 반드시 제한
    @Bean
    public RestClient oauthRestClient(RestClient.Builder builder) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return builder.requestFactory(requestFactory).build();
    }

    // 네이버 검색(이미지, 웹 사진 검색 프록시) 호출용 RestClient
    // 로그인 경로와 달리 사용자가 바텀시트에서 결과를 기다리는 요청이므로 read timeout을 더 짧게 잡는다.
    // 4xx를 예외로 받아 쿼터 초과(429/403)와 그 외를 구분해야 하므로 기본 에러 핸들러를 그대로 둔다.
    @Bean
    public RestClient imageSearchRestClient(RestClient.Builder builder) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(4));

        // NAVER API HUB(NCP)는 body가 JSON인데도 Content-Type을 text/plain으로 내려줄 때가 있다
        // (dev에서 UnknownContentTypeException으로 확인됨). 기본 Jackson 컨버터는 application/json
        // 계열만 처리하므로, text/plain도 JSON으로 파싱하도록 지원 미디어 타입을 넓혀둔다.
        JacksonJsonHttpMessageConverter jsonConverter = new JacksonJsonHttpMessageConverter();
        jsonConverter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN));

        return builder.requestFactory(requestFactory)
                .configureMessageConverters(clientBuilder -> clientBuilder.withJsonConverter(jsonConverter))
                .build();
    }
}
