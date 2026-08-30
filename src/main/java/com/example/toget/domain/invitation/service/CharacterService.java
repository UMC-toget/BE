package com.example.toget.domain.invitation.service;

import com.example.toget.domain.invitation.converter.CharacterConverter;
import com.example.toget.domain.invitation.dto.CharacterCreateResponse;
import com.example.toget.domain.invitation.dto.CharacterRequest;
import com.example.toget.domain.invitation.dto.CharacterResponse;
import com.example.toget.domain.invitation.entity.CharacterEntity;
import com.example.toget.domain.invitation.exception.code.InvitationErrorCode;
import com.example.toget.domain.invitation.exception.InvitationException;
import com.example.toget.domain.invitation.repository.CharacterRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 캐릭터(초대장 스킨) CRUD 비즈니스 로직.
 * 생성/수정/삭제는 관리자용 기능이지만, 현재 role 개념이 없어 인증만 요구.
 * (권한 제한은 SecurityConfig의 TODO 참고 — ADMIN 도입 후 hasRole로 제한 예정)
 */
@Service
@RequiredArgsConstructor
public class CharacterService {

    private final CharacterRepository characterRepository;

    /** 캐릭터 전체 조회 — soft delete된 항목 제외, 없으면 빈 배열 */
    @Transactional(readOnly = true)
    public List<CharacterResponse> getAllCharacters() {
        return characterRepository.findAllByDeletedAtIsNull().stream()
                .map(CharacterConverter::toResponse)
                .toList();
    }

    @Transactional
    public CharacterCreateResponse create(CharacterRequest request) {
        CharacterEntity character = characterRepository.save(CharacterEntity.builder()
                .name(request.name())
                .imageUrl(request.imageUrl())
                .build());
        return CharacterConverter.toCreateResponse(character);
    }

    /** 전체 수정(PUT) — 모든 필드를 요청 값으로 교체 */
    @Transactional
    public CharacterResponse update(Long id, CharacterRequest request) {
        CharacterEntity character = getActiveCharacter(id);
        character.update(request.name(), request.imageUrl()); // dirty checking으로 UPDATE
        return CharacterConverter.toResponse(character);
    }

    /** soft delete — 엔티티의 delete()가 deleted_at만 기록 (실제 DELETE 쿼리 없음) */
    @Transactional
    public void delete(Long id) {
        CharacterEntity character = getActiveCharacter(id);
        character.delete();
    }

    /** 캐릭터 조회 — 없거나 이미 삭제(soft delete)된 경우 404 */
    private CharacterEntity getActiveCharacter(Long id) {
        return characterRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new InvitationException(InvitationErrorCode.CHARACTER_NOT_FOUND));
    }
}
