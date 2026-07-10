package com.example.toget.domain.invitation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 초대장 배경 색상 생성(POST) / 수정(PUT) 요청 DTO.
 * * PUT은 리소스를 전체 교체하는 방식이므로 생성과 동일한 필수 필드를 사용하여 하나의 DTO를 공용으로 사용한다.
 */
public record InvitationBackgroundRequest(
        @NotBlank(message = "name은 필수입니다.")
        @Size(max = 50) // 엔티티 컬럼(length=50)과 맞춰 DB 제약 오류 전에 Validation(400)으로 처리
        String name,

        // 현재는 #RRGGBB 6자리 형식만 허용
        @NotBlank(message = "hexCode는 필수입니다.")
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "hexCode는 #RRGGBB 형식이어야 합니다.")
        String hexCode
) {
}
