package com.example.toget.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 스프링 MVC 동작 커스터마이징 설정.
 * WebMvcConfigurer의 메서드를 오버라이드하면 MVC 기본 설정에 우리 것을 "추가"할 수 있다.
 * 여기서는 @LoginUserId 파라미터를 처리할 커스텀 리졸버를 MVC에 등록한다.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final LoginUserIdArgumentResolver loginUserIdArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserIdArgumentResolver);
    }
}
