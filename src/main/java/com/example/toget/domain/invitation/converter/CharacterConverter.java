package com.example.toget.domain.invitation.converter;

import com.example.toget.domain.invitation.dto.CharacterCreateResponse;
import com.example.toget.domain.invitation.dto.CharacterResponse;
import com.example.toget.domain.invitation.entity.CharacterEntity;

/** CharacterEntity → 응답 DTO 변환 전담 클래스 */
public class CharacterConverter {

    private CharacterConverter() {
    }

    public static CharacterResponse toResponse(CharacterEntity character) {
        return new CharacterResponse(character.getId(), character.getName(), character.getImageUrl());
    }

    public static CharacterCreateResponse toCreateResponse(CharacterEntity character) {
        return new CharacterCreateResponse(character.getId());
    }
}
