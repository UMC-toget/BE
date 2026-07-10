package com.example.toget.domain.invitation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 캐릭터 생성(POST) / 수정(PUT) 요청 본문.
 * PUT은 리소스를 전체 교체하는 방식이므로 생성과 동일한 필수 필드를 사용하여 하나의 DTO를 공용으로 사용한다.
 */
public record CharacterRequest(
        @NotBlank(message = "name은 필수입니다.")
        @Size(max = 50) // 엔티티 컬럼 length와 맞춰 DB 오류 전에 400으로 거른다
        String name,

        @NotBlank(message = "imageUrl은 필수입니다.")
        String imageUrl
) {
}
