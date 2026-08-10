package com.example.toget.domain.bank.dto;

import com.example.toget.global.enums.BankName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "계좌번호 기반 은행 추론 응답 DTO")
public record BankDetectionResponse(
        @Schema(description = "추론된 은행 enum 명칭", example = "KAKAO_BANK")
        BankName bankName,

        @Schema(description = "은행 한국어 표시명", example = "카카오뱅크")
        String displayName,

        @Schema(description = "은행 아이콘 URL (등록되어 있는 경우)", example = "https://...")
        String iconUrl
) {
}
