package com.example.toget.domain.funding.scheduler;

import com.example.toget.domain.funding.service.FundingAutoEndService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * MY_GIFT 참여 종료일 경과 펀딩을 매일 자동으로 마감(ENDED) 처리하는 크론 트리거.
 * <p>
 * 실제 배치 로직(대상 조회 + 상태 전환)은 {@link FundingAutoEndService}에 있고,
 * 이 클래스는 스케줄 등록 및 실행 로그만 담당한다 — 크론 타이밍과 무관하게
 * 배치 로직을 단독으로 테스트할 수 있도록 의도적으로 분리했다.
 * <p>
 * 매일 00:05(KST)에 실행한다. 정각(00:00)이 아니라 5분 여유를 둔 이유는
 * 자정 경계의 다른 배치/서버 클럭 오차와 겹치는 것을 피하기 위함이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FundingAutoEndScheduler {

    private final FundingAutoEndService fundingAutoEndService;

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    public void autoEndExpiredMyGiftFundings() {
        int count = fundingAutoEndService.autoEndExpiredMyGiftFundings();
        log.info("[FundingAutoEndScheduler] 자동 마감 배치 실행 완료 - {}건 처리", count);
    }
}
