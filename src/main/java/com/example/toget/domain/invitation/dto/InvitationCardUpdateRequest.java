package com.example.toget.domain.invitation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 초대장 카드 수정(PUT) 요청 DTO.
 * PUT은 수정 가능한 필드(대표 캐릭터/배경/제목/본문)를 전체 교체하므로 모든 필드가 필수다.
 * title/content의 @Size 상한은 InvitationCard 엔티티의 컬럼 길이 제약과 일치시킨다.
 */
public record InvitationCardUpdateRequest(
        @Schema(description = "대표 캐릭터 ID", example = "2")
        @NotNull(message = "캐릭터 ID는 필수입니다.")
        Long characterId,

        @Schema(description = "배경 색상(색상 테마) ID", example = "3")
        @NotNull(message = "배경 색상 ID는 필수입니다.")
        Long backgroundId,

        @Schema(description = "초대장 제목", example = "수정된 초대장 제목")
        @NotBlank(message = "초대장 제목은 필수입니다.")
        @Size(max = 15, message = "초대장 제목은 15자를 초과할 수 없습니다.")
        String title,

        @Schema(description = "초대장 본문 내용", example = "수정된 초대장 내용입니다.")
        @NotBlank(message = "초대장 본문은 필수입니다.")
        @Size(max = 60, message = "초대장 본문은 60자를 초과할 수 없습니다.")
        String content
) {
}
