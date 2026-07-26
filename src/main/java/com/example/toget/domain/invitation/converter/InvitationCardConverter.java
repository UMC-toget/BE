package com.example.toget.domain.invitation.converter;

import com.example.toget.domain.invitation.dto.InvitationCardResponse;
import com.example.toget.domain.invitation.entity.InvitationCard;

/** InvitationCard → 응답 DTO 변환 전담 클래스 */
public class InvitationCardConverter {

    private InvitationCardConverter() {
    }

    public static InvitationCardResponse toResponse(InvitationCard card) {
        return new InvitationCardResponse(
                card.getId(),
                card.getCharacter().getId(),
                card.getBackground().getId(),
                card.getTitle(),
                card.getContent()
        );
    }
}
