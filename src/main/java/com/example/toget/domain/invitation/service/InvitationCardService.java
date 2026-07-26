package com.example.toget.domain.invitation.service;

import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.repository.FundingRepository;
import com.example.toget.domain.invitation.converter.InvitationCardConverter;
import com.example.toget.domain.invitation.dto.InvitationCardResponse;
import com.example.toget.domain.invitation.dto.InvitationCardUpdateRequest;
import com.example.toget.domain.invitation.entity.CharacterEntity;
import com.example.toget.domain.invitation.entity.InvitationBackground;
import com.example.toget.domain.invitation.entity.InvitationCard;
import com.example.toget.domain.invitation.exception.InvitationException;
import com.example.toget.domain.invitation.exception.code.InvitationErrorCode;
import com.example.toget.domain.invitation.repository.CharacterRepository;
import com.example.toget.domain.invitation.repository.InvitationBackgroundRepository;
import com.example.toget.domain.invitation.repository.InvitationCardRepository;
import com.example.toget.domain.user.service.ActiveUserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 초대장 카드 수정 비즈니스 로직.
 */
@Service
@RequiredArgsConstructor
public class InvitationCardService {

    private final InvitationCardRepository invitationCardRepository;
    private final FundingRepository fundingRepository;
    private final CharacterRepository characterRepository;
    private final InvitationBackgroundRepository invitationBackgroundRepository;
    private final ActiveUserReader activeUserReader;

    /**
     * 특정 펀딩의 초대장 카드 수정 — 대표 캐릭터, 색상 테마, 제목, 본문을 갱신한다.
     * 펀딩 개최자 본인만 수정할 수 있다.
     *
     * @param userId    로그인한 사용자 ID
     * @param fundingId 대상 펀딩 ID
     * @param request   수정 요청 DTO
     * @return 수정 결과 응답 DTO
     * @throws InvitationException 펀딩/초대장 미존재 시 404, 개최자가 아닐 시 403,
     *                             캐릭터/배경 미존재 시 404
     */
    @Transactional
    public InvitationCardResponse update(Long userId, Long fundingId, InvitationCardUpdateRequest request) {
        // 1. 활성 사용자 여부 검증 (보안 컨벤션)
        activeUserReader.getActiveUser(userId);

        // 2. 대상 펀딩 조회 — 없거나 soft delete된 펀딩은 초대장도 없는 것으로 간주(404)
        Funding funding = fundingRepository.findByIdAndDeletedAtIsNull(fundingId)
                .orElseThrow(() -> new InvitationException(InvitationErrorCode.INVITATION_NOT_FOUND));

        // 3. 개최자 권한 검증 — 펀딩 개최자 본인만 수정 가능(403)
        if (!funding.isOwnedBy(userId)) {
            throw new InvitationException(InvitationErrorCode.INVITATION_FORBIDDEN);
        }

        // 4. 해당 펀딩의 초대장 카드 조회 (funding_id UNIQUE로 1:1)
        InvitationCard card = invitationCardRepository.findByFundingId(fundingId)
                .orElseThrow(() -> new InvitationException(InvitationErrorCode.INVITATION_NOT_FOUND));

        // 5. 대표 캐릭터 / 배경(색상 테마) 존재 검증 — soft delete된 항목은 없는 것으로 취급
        CharacterEntity character = characterRepository.findByIdAndDeletedAtIsNull(request.characterId())
                .orElseThrow(() -> new InvitationException(InvitationErrorCode.CHARACTER_NOT_FOUND));
        InvitationBackground background = invitationBackgroundRepository.findByIdAndDeletedAtIsNull(request.backgroundId())
                .orElseThrow(() -> new InvitationException(InvitationErrorCode.BACKGROUND_NOT_FOUND));

        // 6. 필드 갱신 (dirty checking으로 UPDATE)
        card.update(character, background, request.title(), request.content());

        return InvitationCardConverter.toResponse(card);
    }
}
