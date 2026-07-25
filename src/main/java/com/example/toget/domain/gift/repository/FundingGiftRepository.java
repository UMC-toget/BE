package com.example.toget.domain.gift.repository;

import com.example.toget.domain.gift.entity.FundingGift;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * TODO(임시): gift 도메인 담당자님이 FundingGiftRepository를 만드실 예정입니다.
 * POST /fundings 구현을 위해 saveAll()만 필요해 JpaRepository 상속만으로 임시 생성했습니다.
 * 담당자분 버전과 병합/조율 필요 — 이 파일 보시면 말씀 부탁드립니다!
 */
public interface FundingGiftRepository extends JpaRepository<FundingGift, Long> {
}