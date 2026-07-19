package com.example.toget.domain.workspace.repository;

import com.example.toget.domain.workspace.entity.FundingTogetherDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FundingTogetherDraftRepository extends JpaRepository<FundingTogetherDraft, Long> {

    /** 사용자 ID에 매핑된 함께 선물 준비 임시 저장 조회 */
    Optional<FundingTogetherDraft> findByUserId(Long userId);
}
