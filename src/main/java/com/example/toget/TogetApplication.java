package com.example.toget;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing // BaseEntity의 @CreatedDate/@LastModifiedDate 자동 세팅 활성화 (누락 시 created_at NOT NULL 위반으로 INSERT 실패)
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
