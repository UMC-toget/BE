package com.example.toget.global.config;

import com.example.toget.domain.gift.enums.ProductSort;
import com.example.toget.domain.gift.enums.WishlistSort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 스프링 MVC 동작 커스터마이징 설정.
 * WebMvcConfigurer의 메서드를 오버라이드하면 MVC 기본 설정에 우리 것을 "추가"할 수 있다.
 * 여기서는 @LoginUserId 파라미터를 처리할 커스텀 리졸버와
 * @AdminOnly 권한 검사를 수행할 인터셉터, 그리고 Enum 정렬 파라미터 컨버터를 MVC에 등록한다.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final LoginUserIdArgumentResolver loginUserIdArgumentResolver;
    private final AdminOnlyInterceptor adminOnlyInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, ProductSort.class, ProductSort::from);
        registry.addConverter(String.class, WishlistSort.class, WishlistSort::from);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserIdArgumentResolver);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 실제 검사 여부는 인터셉터가 @AdminOnly 유무로 판단하므로 경로는 API 전체로 넓게 잡는다.
        // (Swagger 등 비-API 경로까지 인터셉터를 태울 이유는 없어 /api/** 로 제한)
        registry.addInterceptor(adminOnlyInterceptor)
                .addPathPatterns("/api/**");
        // 실제 검사 여부는 인터셉터가 @RateLimit 유무로 판단하므로 경로는 API 전체로 넓게 잡는다.
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**");
    }
}
