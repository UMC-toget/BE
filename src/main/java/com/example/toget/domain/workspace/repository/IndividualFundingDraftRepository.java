package com.example.toget.domain.workspace.repository;

import com.example.toget.domain.workspace.entity.IndividualFundingDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface IndividualFundingDraftRepository extends JpaRepository<IndividualFundingDraft, Long> {

    /** 유저 아이디로 내 선물 준비 임시 저장 데이터를 조회 */
    Optional<IndividualFundingDraft> findByUserId(Long userId);
}
