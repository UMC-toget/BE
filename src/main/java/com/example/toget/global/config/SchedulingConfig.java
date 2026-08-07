package com.example.toget.global.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Scheduled 기반 배치(스케줄러) 활성화.
 * 최초 도입: MY_GIFT 참여 종료일 경과 펀딩 자동 마감 배치.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

    /**
     * 날짜 계산이 필요한 배치 로직에 주입해서 쓰는 공용 시계.
     * JVM 기본 타임존(전역 상태)에 암묵적으로 기대지 않도록, 명시적으로 KST로 고정한다.
     * 테스트에서는 Clock.fixed(...)로 교체해 원하는 날짜를 결정적으로 지정할 수 있다.
     */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
