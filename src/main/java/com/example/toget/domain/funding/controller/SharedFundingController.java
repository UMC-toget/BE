package com.example.toget.domain.funding.controller;

import com.example.toget.domain.funding.dto.response.SharedFundingDetailResponse;
import com.example.toget.domain.funding.exception.code.FundingSuccessCode;
import com.example.toget.domain.funding.service.FundingQueryService;
import com.example.toget.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 외부 방문자(비회원)용 선물 준비 페이지 조회 API.
 *
 * [설계 포인트]
 *  - 초대장 링크로 들어온 누구나 호출할 수 있어야 하므로 로그인 정보를 받지 않는다.
 *    호출자가 개설자인지 여부도 보지 않는다 — 공개 설정만이 노출 범위를 결정한다.
 *  - 개설자 전용 조회(/fundings/{id}/dashboards/my-gift)와 경로를 분리했다.
 *    같은 화면을 그리지만 "공개 설정을 적용하는가"가 정반대라, 한 엔드포인트에서
 *    권한에 따라 분기하면 실수로 원본이 새어 나갈 여지가 생긴다.
 *  - SecurityConfig에서 /api/v1/shared-fundings/** 가 GET 한정으로 permitAll 처리되어 있다.
 */
@Tag(name = "펀딩 - 외부 방문자용", description = "비회원이 초대장 링크로 방문했을 때 사용하는 공개 API")
@RestController
@RequestMapping("/api/v1/shared-fundings")
@RequiredArgsConstructor
public class SharedFundingController {

    private final FundingQueryService fundingQueryService;

    @Operation(
            summary = "외부 방문자용 선물 준비 상세 조회",
            description = """
                    외부 비회원이 초대장 링크를 통해 방문했을 때 선물 준비 페이지 정보를 반환합니다. \
                    로그인 없이 호출할 수 있습니다.

                    **공개 범위 처리** — 개설자가 지정한 설정에 따라 **서버가 값을 `null`로 내려줍니다.**
                    - `showProgress = false` → `progressRate`
                    - `showAmount = false` → `collectedAmount`
                    - `showParticipantCount = false` → `participantCount`

                    `visibility` 객체를 함께 내려주는 이유는, 값이 `null`일 때 "비공개"인지 \
                    "아직 0원"인지 구분할 수 없기 때문입니다. 안내 문구 노출 판단에 사용하세요.

                    `showParticipantNames`, `showMessages`는 이 API의 응답에 영향을 주지 않습니다. \
                    플래그만 전달하며, 실제 적용은 `GET /api/v1/fundings/{fundingId}/contributions`\
                    (축하 메시지 조회)에서 이뤄집니다.

                    **`status`** 는 MY_GIFT 기준 `SETTLING`(진행 중) / `ENDED`(마감) 두 값만 내려갑니다. \
                    개설자가 종료일과 무관하게 마감할 수 있고 종료일이 지나도 자동 마감되지 않으므로, \
                    날짜로 진행 여부를 판단하지 마세요.

                    **MY_GIFT 전용**입니다. 함께 선물 준비하기는 공개 설정을 사용하지 않아 400이 반환됩니다.
                    """
    )
    @GetMapping("/{fundingId}")
    public ApiResponse<SharedFundingDetailResponse> getSharedFundingDetail(
            @Parameter(description = "펀딩 ID", example = "12") @PathVariable Long fundingId
    ) {
        SharedFundingDetailResponse result = fundingQueryService.getSharedFundingDetail(fundingId);
        return ApiResponse.onSuccess(FundingSuccessCode.FUNDING_DASHBOARD_OK, result);
    }
}
