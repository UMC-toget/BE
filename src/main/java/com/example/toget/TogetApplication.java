package com.example.toget;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// JPA auditing 활성화(@EnableJpaAuditing)는 global.config.JpaAuditingConfig로 분리했다 —
// 이 클래스에 직접 붙이면 @WebMvcTest 등 슬라이스 테스트에서도 강제 적용되어 EntityManagerFactory가
// 없는 컨텍스트에서 부팅이 깨진다. 그 클래스의 주석에 이유를 자세히 적어뒀다.
@ConfigurationPropertiesScan // @ConfigurationProperties 클래스(AdminProperties 등)를 빈으로 등록
@SpringBootApplication
public class TogetApplication {

	// 애플리케이션이 실행될 때 타임존을 한국 시간으로 고정
	@PostConstruct
	public void started() {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
	}

	public static void main(String[] args) {
		SpringApplication.run(TogetApplication.class, args);
	}

}
