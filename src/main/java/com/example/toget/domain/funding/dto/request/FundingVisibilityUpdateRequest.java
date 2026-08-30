package com.example.toget.domain.funding.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 펀딩 공개 설정 수정 요청 DTO.
 *
 * [설계 포인트]
 *  - PUT(전체 교체) 시맨틱이라 5개 필드를 모두 @NotNull로 강제한다.
 *    funding_visibility_settings의 컬럼이 전부 NOT NULL이라, 일부만 보내는 것을 허용하면
 *    나머지 컬럼에 null이 대입되어 DB 제약 위반이 난다.
 *  - 필드명은 펀딩 생성 요청(FundingCreateRequest.VisibilityRequest)과 동일하게 맞춰,
 *    프론트가 생성/수정에서 같은 스키마를 재사용할 수 있게 한다.
 */
public record FundingVisibilityUpdateRequest(

        // 진행률(달성 퍼센트) 공개 여부
        @Schema(description = "진행률(달성 %) 공개 여부", example = "true")
        @NotNull(message = "진행률 공개 여부는 필수입니다.")
        Boolean showProgress,

        // 모금액 공개 여부
        @Schema(description = "모인 총 금액 공개 여부", example = "true")
        @NotNull(message = "모금액 공개 여부는 필수입니다.")
        Boolean showAmount,

        // 참여자 수 공개 여부
        @Schema(description = "참여한 친구 수 공개 여부", example = "false")
        @NotNull(message = "참여자 수 공개 여부는 필수입니다.")
        Boolean showParticipantCount,

        // 참여자 이름 공개 여부
        @Schema(description = "참여자 목록 이름 공개 여부", example = "true")
        @NotNull(message = "참여자 이름 공개 여부는 필수입니다.")
        Boolean showParticipantNames,

        // 축하 메시지 공개 여부
        @Schema(description = "축하 메시지 내용 공개 여부", example = "false")
        @NotNull(message = "축하 메시지 공개 여부는 필수입니다.")
        Boolean showMessages

) {}
