package com.example.toget.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Scheduled 기반 배치(스케줄러) 활성화.
 * 최초 도입: MY_GIFT 참여 종료일 경과 펀딩 자동 마감 배치.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
