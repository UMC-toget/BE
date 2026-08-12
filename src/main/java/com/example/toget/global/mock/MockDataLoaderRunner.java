package com.example.toget.global.mock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 부하 테스트용 대량 더미 데이터 주입 Runner.
 *
 * [실행 방법]
 *  - Spring Boot 실행 시 `--spring.profiles.active=mock-data` 프로필을 활성화하거나
 *    환경 변수 `MOCK_DATA_ENABLED=true`를 전달하면 자동으로 실행됩니다.
 *  - 수량 스케일 조정: `MOCK_DATA_SCALE=1.0` (100% 100만 건 규모), `0.1` (10% 10만 건), `0.01` (1% 1만 건)
 */
@Slf4j
@Component
@Profile("mock-data")
@RequiredArgsConstructor
public class MockDataLoaderRunner implements ApplicationRunner {

    private final MockDataBatchService mockDataBatchService;

    @Value("${mock-data.scale:1.0}")
    private double scale;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info(">>>> [MockDataLoaderRunner] 'mock-data' 프로필이 활성화되었습니다. 더미 데이터 대량 생성을 시작합니다.");
        log.info(">>>> 데이터 주입 스케일: {}x", scale);

        mockDataBatchService.generateAllMockData(scale);

        log.info(">>>> [MockDataLoaderRunner] 모든 더미 데이터 주입이 성공적으로 완료되었습니다!");
    }
}
