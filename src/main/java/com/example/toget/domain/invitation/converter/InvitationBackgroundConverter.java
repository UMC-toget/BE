package com.example.toget.domain.invitation.converter;

import com.example.toget.domain.invitation.dto.InvitationBackgroundCreateResponse;
import com.example.toget.domain.invitation.dto.InvitationBackgroundResponse;
import com.example.toget.domain.invitation.entity.InvitationBackground;

/** InvitationBackground → 응답 DTO 변환 전담 클래스 */
public class InvitationBackgroundConverter {

    private InvitationBackgroundConverter() {
    }

    public static InvitationBackgroundResponse toResponse(InvitationBackground background) {
        return new InvitationBackgroundResponse(background.getId(), background.getName(), background.getHexCode(), background.getSolidColorHex());
    }

    public static InvitationBackgroundCreateResponse toCreateResponse(InvitationBackground background) {
        return new InvitationBackgroundCreateResponse(background.getId());
    }
}
