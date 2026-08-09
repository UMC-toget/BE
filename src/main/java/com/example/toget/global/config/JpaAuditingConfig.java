package com.example.toget.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * BaseEntity의 @CreatedDate/@LastModifiedDate 자동 세팅 활성화.
 * (누락 시 created_at NOT NULL 위반으로 INSERT 실패)
 *
 * TogetApplication(@SpringBootConfiguration)에 직접 붙이지 않고 별도 설정 클래스로 뺀 이유:
 * @WebMvcTest 등 슬라이스 테스트는 컨텍스트 소스로 지정된 @SpringBootConfiguration 클래스의
 * 애노테이션은 슬라이스 여부와 무관하게 항상 그대로 적용한다. @EnableJpaAuditing이 거기 있으면
 * EntityManagerFactory가 없는 웹 슬라이스에서도 JPA auditing 빈(jpaMappingContext 등)을
 * 만들려다 실패한다. 일반 @Configuration으로 분리하면 컴포넌트 스캔 대상이라 전체 부팅 시엔
 * 그대로 동작하고, 슬라이스 테스트에서는 자연히 제외된다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
