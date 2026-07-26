package com.example.toget.domain.invitation.repository;

import com.example.toget.domain.invitation.entity.InvitationCard;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationCardRepository extends JpaRepository<InvitationCard, Long> {

    // 펀딩:초대장은 1:1 (funding_id UNIQUE) — 특정 펀딩의 초대장 단건 조회
    Optional<InvitationCard> findByFundingId(Long fundingId);
}
