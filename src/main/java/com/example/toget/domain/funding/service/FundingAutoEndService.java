package com.example.toget.domain.funding.service;

import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.enums.FundingStatus;
import com.example.toget.domain.funding.enums.FundingType;
import com.example.toget.domain.funding.repository.FundingRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MY_GIFT 참여 종료일 경과 펀딩 자동 마감(ENDED) 처리.
 * <p>
 * 기능명세서: "참여 종료일이 지나면 페이지를 자동으로 기간 종료 상태로 변경한다."
 * TOGETHER_GIFT는 개설자가 직접 상태를 전환하는 방식을 그대로 유지하므로 대상에서 제외한다
 * (기념일까지 선물 전달이 완료되지 않을 수 있어 날짜 기반 자동 전환을 두지 않기로).
 * <p>
 * 스케줄 트리거(@Scheduled)는 이 클래스가 아니라 별도 스케줄러 컴포넌트에서 이 메서드를 호출한다 —
 * 크론 타이밍과 무관하게 배치 로직만 단독으로 테스트할 수 있도록 분리했다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FundingAutoEndService {

    private final FundingRepository fundingRepository;

    /**
     * 오늘(자정 기준) 종료일이 지났는데도 SETTLING 상태인 MY_GIFT 펀딩을 전부 ENDED로 전환한다.
     *
     * @return 실제로 마감 처리된 펀딩 건수
     */
    @Transactional
    public int autoEndExpiredMyGiftFundings() {
        List<Funding> targets = fundingRepository
                .findAllByFundingTypeAndStatusAndEndDateBeforeAndDeletedAtIsNull(
                        FundingType.MY_GIFT, FundingStatus.SETTLING, LocalDate.now());

        targets.forEach(Funding::complete);

        if (!targets.isEmpty()) {
            log.info("[FundingAutoEnd] 참여 종료일 경과로 MY_GIFT {}건 자동 마감(ENDED) 처리", targets.size());
        }

        return targets.size();
    }
}
