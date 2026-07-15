package com.example.toget.domain.workspace.service;

import com.example.toget.domain.user.entity.UserAccount;
import com.example.toget.domain.user.repository.UserAccountRepository;
import com.example.toget.domain.user.service.ActiveUserReader;
import com.example.toget.domain.workspace.converter.FundingTogetherDraftConverter;
import com.example.toget.domain.workspace.dto.FundingTogetherDraftDetailResponse;
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
        UserAccount userAccount = null;
        if (draft.getUserAccountId() != null) {
            userAccount = userAccountRepository.findById(draft.getUserAccountId()).orElse(null);
        }

        // 4. DTO 변환 및 반환
        return FundingTogetherDraftConverter.toDetailResponse(draft, userAccount);
    }
}
