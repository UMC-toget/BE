package com.example.toget.domain.gift.repository;

import com.example.toget.domain.gift.entity.FundingGiftComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FundingGiftCommentRepository extends JpaRepository<FundingGiftComment, Long> {

    List<FundingGiftComment> findAllByFundingGiftIdOrderByCreatedAtAsc(Long fundingGiftId);
}