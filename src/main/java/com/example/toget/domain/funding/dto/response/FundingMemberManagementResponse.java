package com.example.toget.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record FundingMemberManagementResponse(

        // 최대 50명 제한이기 때문에 Long 불필요
        @Schema(description = "전체 참여자 수")
        int totalCount,

        @Schema(description = "관리자 수 (개설자 포함)")
        int adminCount,

        @Schema(description = "일반 참여자 수")
        int participantCount,

        @Schema(description = "개설자+관리자 목록")
        List<MemberInfo> admins,

        @Schema(description = "일반 참여자 목록")
        List<MemberInfo> participants
) {
    public record MemberInfo(
            Long fundingMemberId,
            Long userId,
            String name,
            String profileImageUrl,
            String role
    ) {}
}