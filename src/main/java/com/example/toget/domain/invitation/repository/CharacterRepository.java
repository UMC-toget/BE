package com.example.toget.domain.invitation.repository;

import com.example.toget.domain.invitation.entity.CharacterEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterRepository extends JpaRepository<CharacterEntity, Long> {

    // soft delete 정책: deleted_at이 기록된(삭제된) 캐릭터는 조회 대상에서 제외
    List<CharacterEntity> findAllByDeletedAtIsNull();

    // 수정/삭제 대상 조회용 — 이미 soft delete된 캐릭터는 없는 것으로 취급(404)
    Optional<CharacterEntity> findByIdAndDeletedAtIsNull(Long id);
}
