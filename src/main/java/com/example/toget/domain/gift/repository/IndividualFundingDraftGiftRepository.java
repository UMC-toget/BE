package com.example.toget.domain.gift.repository;

import com.example.toget.domain.gift.entity.IndividualFundingDraftGift;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IndividualFundingDraftGiftRepository extends JpaRepository<IndividualFundingDraftGift, Long> {

    /** 임시 저장 ID에 매핑된 선물 후보 목록 전체 조회 */
    List<IndividualFundingDraftGift> findAllByMyDraftId(Long myDraftId);

    /** 임시 저장 ID에 매핑된 선물 후보 목록 전체 삭제 */
    void deleteByMyDraftId(Long myDraftId);
}
