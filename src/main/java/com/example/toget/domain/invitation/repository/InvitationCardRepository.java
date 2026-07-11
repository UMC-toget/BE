package com.example.toget.domain.invitation.repository;

import com.example.toget.domain.invitation.entity.InvitationCard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationCardRepository extends JpaRepository<InvitationCard, Long> {
}
