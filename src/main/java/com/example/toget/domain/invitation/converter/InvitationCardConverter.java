package com.example.toget.domain.invitation.converter;

import com.example.toget.domain.invitation.dto.InvitationCardResponse;
import com.example.toget.domain.invitation.entity.InvitationCard;

/** InvitationCard → 응답 DTO 변환 전담 클래스 */
public class InvitationCardConverter {

    private InvitationCardConverter() {
    }

    /** 수정 결과 응답 — 개설자 본인이 호출하므로 creatorName은 담지 않는다 */
    public static InvitationCardResponse toResponse(InvitationCard card) {
        return toDetailResponse(card, null);
    }

    /**
     * 조회 결과 응답 — 화면의 "from. OO" 표기에 쓸 개설자 이름을 함께 담는다.
     *
     * @param creatorName 개설자 이름. 개설자 정보를 찾을 수 없으면 null이며,
     *                    이때 FE는 해당 영역을 표시하지 않는다.
     */
    public static InvitationCardResponse toDetailResponse(InvitationCard card, String creatorName) {
        return new InvitationCardResponse(
                card.getId(),
                creatorName,
                card.getCharacter().getId(),
                card.getBackground().getId(),
                card.getTitle(),
                card.getContent()
        );
    }
}
