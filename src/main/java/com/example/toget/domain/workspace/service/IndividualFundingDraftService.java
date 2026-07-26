package com.example.toget.domain.workspace.service;

import com.example.toget.domain.gift.entity.IndividualFundingDraftGift;
import com.example.toget.domain.gift.repository.IndividualFundingDraftGiftRepository;
import com.example.toget.domain.invitation.entity.CharacterEntity;
import com.example.toget.domain.invitation.entity.InvitationBackground;
import com.example.toget.domain.invitation.exception.code.InvitationErrorCode;
import com.example.toget.domain.invitation.exception.InvitationException;
import com.example.toget.domain.invitation.repository.CharacterRepository;
import com.example.toget.domain.invitation.repository.InvitationBackgroundRepository;
import com.example.toget.domain.user.entity.UserAccount;
import com.example.toget.domain.user.exception.UserErrorCode;
import com.example.toget.domain.user.exception.UserException;
import com.example.toget.domain.user.repository.UserAccountRepository;
import com.example.toget.domain.user.service.ActiveUserReader;
import com.example.toget.domain.workspace.converter.IndividualFundingDraftConverter;
import com.example.toget.domain.workspace.dto.IndividualFundingDraftDetailResponse;
import com.example.toget.domain.workspace.dto.IndividualFundingDraftSaveRequest;
import com.example.toget.domain.workspace.dto.IndividualFundingDraftSaveResponse;
import com.example.toget.domain.workspace.entity.IndividualFundingDraft;
import com.example.toget.domain.workspace.exception.code.WorkspaceErrorCode;
import com.example.toget.domain.workspace.exception.WorkspaceException;
import com.example.toget.domain.workspace.repository.IndividualFundingDraftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 내 선물 준비 임시 작성 서비스 비즈니스 로직.
 */
@Service
@RequiredArgsConstructor
public class IndividualFundingDraftService {

    private final IndividualFundingDraftRepository individualFundingDraftRepository;
    private final IndividualFundingDraftGiftRepository individualFundingDraftGiftRepository;
    private final UserAccountRepository userAccountRepository;
    private final CharacterRepository characterRepository;
    private final InvitationBackgroundRepository invitationBackgroundRepository;
    private final ActiveUserReader activeUserReader;

    /**
     * 내 선물 준비 임시 저장 상세 정보 및 매핑된 선물 리스트 결합 조회
     * @param userId 로그인한 사용자 ID
     * @return 상세 조회 응답 DTO
     * @throws WorkspaceException 임시 저장 데이터가 존재하지 않을 시 DRAFT404 예외 발생
     */
    @Transactional(readOnly = true)
    public IndividualFundingDraftDetailResponse getDetail(Long userId) {
        // 1. 활성 사용자 여부 검증 (보안 컨벤션)
        activeUserReader.getActiveUser(userId);

        // 2. 임시 저장 데이터 조회 (존재하지 않을 시 WorkspaceException(DRAFT404) 발생)
        IndividualFundingDraft draft = individualFundingDraftRepository.findByUserId(userId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.DRAFT_NOT_FOUND));

        // 3. 연동된 선물 리스트 조회
        List<IndividualFundingDraftGift> gifts = individualFundingDraftGiftRepository.findAllByMyDraftId(draft.getId());

        // 4. 연동된 계좌 정보 조회 (있을 경우)
        UserAccount userAccount = null;
        if (draft.getUserAccountId() != null) {
            userAccount = userAccountRepository.findById(draft.getUserAccountId()).orElse(null);
        }

        // 5. DTO 변환 및 반환
        return IndividualFundingDraftConverter.toDetailResponse(draft, gifts, userAccount);
    }

    /**
     * 내 선물 준비 임시 저장 (생성 또는 업데이트)
     * @param userId 로그인한 사용자 ID
     * @param request 임시 저장 요청 DTO
     * @return 임시 저장 결과 DTO (draft ID)
     */
    @Transactional
    public IndividualFundingDraftSaveResponse save(Long userId, IndividualFundingDraftSaveRequest request) {
        // 1. 활성 사용자 여부 검증 (보안 컨벤션)
        activeUserReader.getActiveUser(userId);

        // 2. 날짜 순서 검증 (시작일이 종료일보다 늦을 수 없음)
        if (request.startDate() != null && request.endDate() != null) {
            if (request.startDate().isAfter(request.endDate())) {
                throw new WorkspaceException(WorkspaceErrorCode.INVALID_DATE_RANGE);
            }
        }

        // 3. 캐릭터 및 배경 정보 검증 및 조회
        CharacterEntity character = null;
        if (request.invitationCard() != null && request.invitationCard().characterId() != null) {
            character = characterRepository.findById(request.invitationCard().characterId())
                    .orElseThrow(() -> new InvitationException(InvitationErrorCode.CHARACTER_NOT_FOUND));
        }

        InvitationBackground background = null;
        if (request.invitationCard() != null && request.invitationCard().backgroundId() != null) {
            background = invitationBackgroundRepository.findById(request.invitationCard().backgroundId())
                    .orElseThrow(() -> new InvitationException(InvitationErrorCode.BACKGROUND_NOT_FOUND));
        }

        // 4. 계좌 처리
        Long userAccountId = null;
        if (request.userAccountId() != null) {
            UserAccount userAccount = userAccountRepository.findById(request.userAccountId())
                    .orElseThrow(() -> new UserException(UserErrorCode.ACCOUNT_NOT_FOUND));
            if (!userAccount.isOwnedBy(userId)) {
                throw new UserException(UserErrorCode.ACCOUNT_FORBIDDEN);
            }
            userAccountId = userAccount.getId();
        }

        // 5. 기존 임시 저장 데이터가 있는지 조회
        IndividualFundingDraft draft = individualFundingDraftRepository.findByUserId(userId).orElse(null);

        // 공개 범위 설정 매핑
        Boolean isProgressPublic = (request.visibilitySettings() != null && request.visibilitySettings().showProgress() != null) ? request.visibilitySettings().showProgress() : true;
        Boolean isAmountPublic = (request.visibilitySettings() != null && request.visibilitySettings().showAmount() != null) ? request.visibilitySettings().showAmount() : true;
        Boolean isParticipantCountPublic = (request.visibilitySettings() != null && request.visibilitySettings().showParticipantCount() != null) ? request.visibilitySettings().showParticipantCount() : true;
        Boolean isParticipantNamePublic = (request.visibilitySettings() != null && request.visibilitySettings().showParticipantNames() != null) ? request.visibilitySettings().showParticipantNames() : true;
        Boolean isMessagePublic = (request.visibilitySettings() != null && request.visibilitySettings().showMessages() != null) ? request.visibilitySettings().showMessages() : true;

        String invitationTitle = request.invitationCard() != null ? request.invitationCard().title() : null;
        String invitationContent = request.invitationCard() != null ? request.invitationCard().content() : null;

        if (draft == null) {
            // 새로 생성
            draft = IndividualFundingDraftConverter.toEntity(userId, request, character, background, userAccountId);
            draft = individualFundingDraftRepository.save(draft);
        } else {
            // 기존 객체 필드 업데이트 (Dirty checking으로 UPDATE)
            draft.update(
                    request.step(),
                    request.title(),
                    request.anniversaryDate(),
                    request.startDate(),
                    request.endDate(),
                    request.greeting(),
                    request.thumbnailUrl(),
                    userAccountId,
                    isProgressPublic,
                    isAmountPublic,
                    isParticipantCountPublic,
                    isParticipantNamePublic,
                    isMessagePublic,
                    character,
                    background,
                    invitationTitle,
                    invitationContent
            );
        }

        // 5. 연동된 기존 선물 리스트 삭제 후 새 선물 리스트 등록 (영속성 전이 UPSERT 대응)
        individualFundingDraftGiftRepository.deleteByMyDraftId(draft.getId());

        if (request.gifts() != null) {
            final Long draftId = draft.getId();
            List<IndividualFundingDraftGift> giftsToSave = request.gifts().stream()
                    .map(giftReq -> IndividualFundingDraftConverter.toDraftGiftEntity(draftId, giftReq))
                    .toList();
            individualFundingDraftGiftRepository.saveAll(giftsToSave);
        }

        return new IndividualFundingDraftSaveResponse(draft.getId());
    }

    /**
     * 내 선물 준비 임시 저장 삭제 및 연동 선물 리스트 cascade 삭제
     * @param userId 로그인한 사용자 ID
     */
    @Transactional
    public void delete(Long userId) {
        // 1. 활성 사용자 여부 검증 (보안 컨벤션)
        activeUserReader.getActiveUser(userId);

        // 2. 삭제할 Draft 조회 (존재하지 않을 시 DRAFT404 예외 발생)
        IndividualFundingDraft draft = individualFundingDraftRepository.findByUserId(userId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.DRAFT_NOT_FOUND));

        // 3. 연동된 선물 리스트 일괄 삭제
        individualFundingDraftGiftRepository.deleteByMyDraftId(draft.getId());

        // 4. Draft 삭제
        individualFundingDraftRepository.delete(draft);
    }
}
