package com.example.toget.domain.invitation.service;

import com.example.toget.domain.invitation.converter.InvitationBackgroundConverter;
import com.example.toget.domain.invitation.dto.InvitationBackgroundCreateResponse;
import com.example.toget.domain.invitation.dto.InvitationBackgroundRequest;
import com.example.toget.domain.invitation.dto.InvitationBackgroundResponse;
import com.example.toget.domain.invitation.entity.InvitationBackground;
import com.example.toget.domain.invitation.exception.code.InvitationErrorCode;
import com.example.toget.domain.invitation.exception.InvitationException;
import com.example.toget.domain.invitation.repository.InvitationBackgroundRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 초대장 배경 색상 CRUD 비즈니스 로직.
 * 생성/수정/삭제는 관리자용 기능이지만, 현재 role 개념이 없어 인증만 요구.
 * (권한 제한은 SecurityConfig의 TODO 참고 — ADMIN 도입 후 hasRole로 제한 예정)
 */
@Service
@RequiredArgsConstructor
public class InvitationBackgroundService {

    private final InvitationBackgroundRepository invitationBackgroundRepository;

    /** 배경 색상 전체 조회 — soft delete된 항목 제외, 없으면 빈 배열 */
    @Transactional(readOnly = true)
    public List<InvitationBackgroundResponse> getAllBackgrounds() {
        return invitationBackgroundRepository.findAllByDeletedAtIsNull().stream()
                .map(InvitationBackgroundConverter::toResponse)
                .toList();
    }

    @Transactional
    public InvitationBackgroundCreateResponse create(InvitationBackgroundRequest request) {
        InvitationBackground background = invitationBackgroundRepository.save(InvitationBackground.builder()
                .name(request.name())
                .hexCode(request.hexCode())
                .build());
        return InvitationBackgroundConverter.toCreateResponse(background);
    }

    /** 전체 수정(PUT) — 모든 필드를 요청 값으로 교체 */
    @Transactional
    public InvitationBackgroundResponse update(Long id, InvitationBackgroundRequest request) {
        InvitationBackground background = getActiveBackground(id);
        background.update(request.name(), request.hexCode()); // dirty checking으로 UPDATE
        return InvitationBackgroundConverter.toResponse(background);
    }

    /** soft delete — 엔티티의 delete()가 deleted_at만 기록 (실제 DELETE 쿼리 없음) */
    @Transactional
    public void delete(Long id) {
        InvitationBackground background = getActiveBackground(id);
        background.delete();
    }

    /** 배경 조회 — 없거나 이미 삭제(soft delete)된 경우 404 */
    private InvitationBackground getActiveBackground(Long id) {
        return invitationBackgroundRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new InvitationException(InvitationErrorCode.BACKGROUND_NOT_FOUND));
    }
}
