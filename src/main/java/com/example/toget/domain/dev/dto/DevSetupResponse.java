package com.example.toget.domain.dev.dto;

import com.example.toget.domain.funding.enums.FundingStatus;
import com.example.toget.domain.user.dto.TokenResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DevSetupResponse {

    private UserTokenInfo creator;
    private UserTokenInfo coManager;
    private UserTokenInfo participant;
    private List<FundingTestInfoDto> fundings;

    @Getter
    @Builder
    public static class UserTokenInfo {
        private String roleDescription;
        private Long userId;
        private String email;
        private String accessToken;
        private String refreshToken;

        public static UserTokenInfo of(String roleDescription, Long userId, String email, TokenResponse tokenResponse) {
            return UserTokenInfo.builder()
                    .roleDescription(roleDescription)
                    .userId(userId)
                    .email(email)
                    .accessToken(tokenResponse.accessToken())
                    .refreshToken(tokenResponse.refreshToken())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class FundingTestInfoDto {
        private Long fundingId;
        private FundingStatus status;
        private String title;
    }
}
