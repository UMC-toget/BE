package com.example.toget.domain.dev.service;

import com.example.toget.domain.dev.dto.DevSetupResponse;
import com.example.toget.domain.dev.dto.DevSetupResponse.FundingTestInfoDto;
import com.example.toget.domain.dev.dto.DevSetupResponse.UserTokenInfo;
import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.entity.FundingMember;
import com.example.toget.domain.funding.enums.FundingStatus;
import com.example.toget.domain.funding.enums.FundingType;
import com.example.toget.domain.funding.repository.FundingMemberRepository;
import com.example.toget.domain.funding.repository.FundingRepository;
import com.example.toget.domain.user.dto.TokenResponse;
import com.example.toget.domain.user.entity.User;
import com.example.toget.domain.user.enums.OAuthProvider;
import com.example.toget.domain.user.repository.UserRepository;
import com.example.toget.domain.user.service.TokenIssuer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Profile({"local", "dev"})
@Service
@RequiredArgsConstructor
public class DevSetupService {

    private final UserRepository userRepository;
    private final FundingRepository fundingRepository;
    private final FundingMemberRepository fundingMemberRepository;
    private final TokenIssuer tokenIssuer;

    @Transactional
    public DevSetupResponse setupTestData() {
        // 1. 유저 3명 생성 또는 조회 (개설자, 공동관리자, 참여자)
        User creator = getOrCreateDevUser("dev-creator-oauth-id", "creator@dev.com", "개설자", "개설자닉네임");
        User admin = getOrCreateDevUser("dev-admin-oauth-id", "admin@dev.com", "공동관리자", "공동관리자닉네임");
        User participant = getOrCreateDevUser("dev-participant-oauth-id", "participant@dev.com", "참여자", "참여자닉네임");

        // 2. 각 유저별 Access Token + Refresh Token 발급
        TokenResponse creatorTokens = tokenIssuer.issue(creator);
        TokenResponse adminTokens = tokenIssuer.issue(admin);
        TokenResponse participantTokens = tokenIssuer.issue(participant);

        // 3. 5개 상태별 함께선물(TOGETHER_GIFT) 펀딩 생성 및 3명 멤버 자동 배정
        FundingStatus[] statuses = {
                FundingStatus.SELECTING,
                FundingStatus.SETTLING,
                FundingStatus.PURCHASING,
                FundingStatus.DELIVERING,
                FundingStatus.ENDED
        };

        List<FundingTestInfoDto> fundingTestInfos = new ArrayList<>();

        for (FundingStatus status : statuses) {
            Funding funding = Funding.builder()
                    .userId(creator.getId())
                    .fundingType(FundingType.TOGETHER_GIFT)
                    .title("[" + status.name() + "] 함께선물 검증용 펀딩")
                    .recipientName("선물 수령인")
                    .anniversaryDate(LocalDate.now().plusDays(10))
                    .startDate(LocalDate.now().minusDays(1))
                    .endDate(LocalDate.now().plusDays(7))
                    .introduction("검증용 함께선물 펀딩입니다.")
                    .targetAmount(100000L)
                    .status(status)
                    .build();

            fundingRepository.save(funding);

            // 3명 계정을 개설자/공동관리자/참여자로 등록
            fundingMemberRepository.save(FundingMember.createCreator(funding.getId(), creator.getId()));

            FundingMember adminMember = FundingMember.createParticipant(funding.getId(), admin.getId());
            adminMember.promoteToAdmin();
            fundingMemberRepository.save(adminMember);

            fundingMemberRepository.save(FundingMember.createParticipant(funding.getId(), participant.getId()));

            fundingTestInfos.add(FundingTestInfoDto.builder()
                    .fundingId(funding.getId())
                    .status(status)
                    .title(funding.getTitle())
                    .build());
        }

        return DevSetupResponse.builder()
                .creator(UserTokenInfo.of("개설자", creator.getId(), creator.getEmail(), creatorTokens))
                .coManager(UserTokenInfo.of("공동관리자", admin.getId(), admin.getEmail(), adminTokens))
                .participant(UserTokenInfo.of("참여자", participant.getId(), participant.getEmail(), participantTokens))
                .fundings(fundingTestInfos)
                .build();
    }

    private User getOrCreateDevUser(String oauthId, String email, String name, String nickname) {
        return userRepository.findByOAuthProviderAndOAuthId(OAuthProvider.KAKAO, oauthId)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .oAuthProvider(OAuthProvider.KAKAO)
                                .oAuthId(oauthId)
                                .email(email)
                                .name(name)
                                .nickname(nickname)
                                .profileImageUrl(null)
                                .build()
                ));
    }
}
