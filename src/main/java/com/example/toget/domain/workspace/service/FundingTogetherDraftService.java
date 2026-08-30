package com.example.toget.domain.workspace.service;

import com.example.toget.domain.user.entity.UserAccount;
import com.example.toget.domain.user.exception.UserErrorCode;
import com.example.toget.domain.user.exception.UserException;
import com.example.toget.domain.user.repository.UserAccountRepository;
import com.example.toget.domain.user.service.ActiveUserReader;
import com.example.toget.domain.workspace.converter.FundingTogetherDraftConverter;
import com.example.toget.domain.workspace.dto.FundingTogetherDraftDetailResponse;
import com.example.toget.domain.workspace.dto.FundingTogetherDraftSaveRequest;
import com.example.toget.domain.workspace.dto.FundingTogetherDraftSaveResponse;
import com.example.toget.domain.workspace.entity.FundingTogetherDraft;
import com.example.toget.domain.workspace.exception.WorkspaceException;
import com.example.toget.domain.workspace.exception.code.WorkspaceErrorCode;
import com.example.toget.domain.workspace.repository.FundingTogetherDraftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 함께 선물 준비 임시 작성 서비스 비즈니스 로직.
 */
@Service
@RequiredArgsConstructor
public class FundingTogetherDraftService {

    private final FundingTogetherDraftRepository fundingTogetherDraftRepository;
    private final UserAccountRepository userAccountRepository;
    private final ActiveUserReader activeUserReader;

    /**
     * 함께 선물 준비 임시 저장 상세 정보 조회
     * @param userId 로그인한 사용자 ID
     * @return 상세 조회 응답 DTO
     * @throws WorkspaceException 임시 저장 데이터가 존재하지 않을 시 DRAFT404 예외 발생
     */
    @Transactional(readOnly = true)
    public FundingTogetherDraftDetailResponse getDetail(Long userId) {
        // 1. 활성 사용자 여부 검증 (보안 컨벤션)
        activeUserReader.getActiveUser(userId);

        // 2. 임시 저장 데이터 조회 (존재하지 않을 시 WorkspaceException(DRAFT404) 발생)
        FundingTogetherDraft draft = fundingTogetherDraftRepository.findByUserId(userId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.DRAFT_NOT_FOUND));

        // 3. 연동된 계좌 정보 조회 (있을 경우)
        //    응답에 은행 아이콘이 실리므로 bank를 함께 읽는다(findWithBankById).
        //    일반 findById를 쓰면 LAZY인 bank 때문에 은행 SELECT가 한 번 더 나간다.
        UserAccount userAccount = null;
        if (draft.getUserAccountId() != null) {
            userAccount = userAccountRepository.findWithBankById(draft.getUserAccountId()).orElse(null);
        }

        // 4. DTO 변환 및 반환
        return FundingTogetherDraftConverter.toDetailResponse(draft, userAccount);
    }

    /**
     * 함께 선물 준비 임시 저장 (생성 또는 업데이트)
     * @param userId 로그인한 사용자 ID
     * @param request 임시 저장 요청 DTO
     * @return 임시 저장 결과 DTO
     */
    @Transactional
    public FundingTogetherDraftSaveResponse save(Long userId, FundingTogetherDraftSaveRequest request) {
        // 1. 활성 사용자 여부 검증 (보안 컨벤션)
        activeUserReader.getActiveUser(userId);

        // 2. 날짜 순서 검증 (시작일이 종료일보다 늦을 수 없음)
        if (request.startDate() != null && request.endDate() != null) {
            if (request.startDate().isAfter(request.endDate())) {
                throw new WorkspaceException(WorkspaceErrorCode.INVALID_DATE_RANGE);
            }
        }

        // 3. 종료일과 전달일 순서 검증 (종료일이 선물 전달 날짜보다 늦을 수 없음)
        if (request.endDate() != null && request.anniversaryDate() != null) {
            if (request.endDate().isAfter(request.anniversaryDate())) {
                throw new WorkspaceException(WorkspaceErrorCode.INVALID_END_DATE);
            }
        }

        // 4. 계좌 처리 및 소유권 확인
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
        FundingTogetherDraft draft = fundingTogetherDraftRepository.findByUserId(userId).orElse(null);

        if (draft == null) {
            // 새로 생성 (Converter 적용)
            draft = FundingTogetherDraftConverter.toEntity(userId, request, userAccountId);
            draft = fundingTogetherDraftRepository.save(draft);
        } else {
            // 기존 객체 필드 업데이트 (Dirty checking)
            String cardTitle = request.invitationCard() != null ? request.invitationCard().title() : null;
            String cardContent = request.invitationCard() != null ? request.invitationCard().content() : null;
            draft.update(
                    request.step(),
                    request.startDate(),
                    request.endDate(),
                    request.title(),
                    request.receiver(),
                    request.anniversaryDate(),
                    request.description(),
                    request.thumbnailImageUrl(),
                    userAccountId,
                    cardTitle,
                    cardContent
            );
        }

        return new FundingTogetherDraftSaveResponse(draft.getId());
    }

    /**
     * 함께 선물 준비 임시 저장 삭제
     * @param userId 로그인한 사용자 ID
     * @param draftId 삭제 대상 임시 저장 ID
     */
    @Transactional
    public void delete(Long userId, Long draftId) {
        // 1. 활성 사용자 여부 검증 (보안 컨벤션)
        activeUserReader.getActiveUser(userId);

        // 2. 삭제할 Draft 조회 (존재하지 않을 시 DRAFT404 예외 발생)
        FundingTogetherDraft draft = fundingTogetherDraftRepository.findById(draftId)
                .orElseThrow(() -> new WorkspaceException(WorkspaceErrorCode.DRAFT_NOT_FOUND));

        // 3. 소유권 검증
        if (!draft.getUserId().equals(userId)) {
            throw new WorkspaceException(WorkspaceErrorCode.DRAFT_FORBIDDEN);
        }

        // 4. Draft 삭제
        fundingTogetherDraftRepository.delete(draft);
    }
}
