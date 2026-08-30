package com.example.toget.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 펀딩 공개 설정 수정 응답 DTO.
 *
 * [설계 포인트]
 *  - 필드 순서를 FundingMyGiftDashboardResponse.VisibilityInfo와 동일하게 맞췄다.
 *    프론트가 개설자 대시보드와 이 응답을 같은 화면에서 다루기 때문.
 */
public record FundingVisibilityUpdateResponse(

        // funding_visibility_settings PK
        @Schema(description = "공개 설정 ID", example = "1")
        Long fundingVisibilitySettingId,

        @Schema(description = "진행률(달성 %) 공개 여부", example = "true")
        Boolean showProgress,

        @Schema(description = "모인 총 금액 공개 여부", example = "true")
        Boolean showAmount,

        @Schema(description = "참여한 친구 수 공개 여부", example = "false")
        Boolean showParticipantCount,

        @Schema(description = "참여자 목록 이름 공개 여부", example = "true")
        Boolean showParticipantNames,

        @Schema(description = "축하 메시지 내용 공개 여부", example = "false")
        Boolean showMessages

) {}
