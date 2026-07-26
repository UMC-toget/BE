package com.example.toget.domain.funding.dto.request;

import com.example.toget.domain.funding.enums.FundingRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record FundingMemberRoleUpdateRequest(
        @Schema(description = "변경할 역할 (ADMIN 또는 PARTICIPANT)", example = "ADMIN")
        @NotNull(message = "역할은 필수입니다.")
        FundingRole role
) {}