package com.example.toget.domain.invitation.repository;

import com.example.toget.domain.invitation.entity.CharacterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterRepository extends JpaRepository<CharacterEntity, Long> {
}
