package com.example.toget.domain.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.time.LocalDate;

public record FundingTogetherDraftSaveRequest(
        @Schema(description = "임시저장 단계", example = "1")
        Integer step,

        @Schema(description = "펀딩 제목", example = "우리 부모님 환갑 펀딩")
        String title,

        @Schema(description = "받는 사람 이름", example = "김부모")
        String receiver,

        @Schema(description = "기념일 날짜", example = "2026-08-15")
        LocalDate anniversaryDate,

        @Schema(description = "펀딩 시작일", example = "2026-08-01")
        LocalDate startDate,

        @Schema(description = "펀딩 종료일", example = "2026-08-14")
        LocalDate endDate,

        @Schema(description = "펀딩 설명 및 내용", example = "부모님 환갑을 축하하기 위해 함께 힘을 모아 선물을 준비해요!")
        String description,

        @Schema(description = "펀딩 대표 썸네일 이미지 URL", example = "https://toget.com/images/thumbnail.png")
        String thumbnailImageUrl,

        @Schema(description = "지정할 정산용 사용자 계좌 ID", example = "1")
        Long userAccountId,

        @Schema(description = "초대장 카드 내용 설정")
        @Valid InvitationCardRequest invitationCard
) {
    public record InvitationCardRequest(
            @Schema(description = "초대장 제목", example = "환갑 잔치에 초대합니다")
            String title,

            @Schema(description = "초대장 본문 내용", example = "사랑하는 부모님의 환갑 축하 자리에 소중한 분들을 초대합니다.")
            String content
    ) {}
}
