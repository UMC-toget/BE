package com.example.toget.domain.dev.service;

import com.example.toget.domain.dev.dto.DevSetupResponse;
import com.example.toget.domain.dev.dto.DevSetupResponse.FundingTestInfoDto;
import com.example.toget.domain.dev.dto.DevSetupResponse.UserTokenInfo;
import com.example.toget.domain.funding.entity.ContributionBackground;
import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.entity.FundingContribution;
import com.example.toget.domain.funding.entity.FundingMember;
import com.example.toget.domain.funding.entity.FundingVisibilitySettings;
import com.example.toget.domain.funding.enums.FundingStatus;
import com.example.toget.domain.funding.enums.FundingType;
import com.example.toget.domain.funding.exception.ContributionException;
import com.example.toget.domain.funding.exception.FundingException;
import com.example.toget.domain.funding.exception.code.ContributionErrorCode;
import com.example.toget.domain.funding.exception.code.FundingErrorCode;
import com.example.toget.domain.funding.repository.ContributionBackgroundRepository;
import com.example.toget.domain.funding.repository.FundingContributionRepository;
import com.example.toget.domain.funding.repository.FundingMemberRepository;
import com.example.toget.domain.funding.repository.FundingRepository;
import com.example.toget.domain.funding.repository.FundingVisibilitySettingsRepository;
import com.example.toget.domain.invitation.entity.CharacterEntity;
import com.example.toget.domain.invitation.entity.InvitationBackground;
import com.example.toget.domain.invitation.entity.InvitationCard;
import com.example.toget.domain.invitation.repository.CharacterRepository;
import com.example.toget.domain.invitation.repository.InvitationBackgroundRepository;
import com.example.toget.domain.invitation.repository.InvitationCardRepository;
import com.example.toget.domain.user.dto.TokenResponse;
import com.example.toget.domain.user.entity.User;
import com.example.toget.domain.user.entity.UserAccount;
import com.example.toget.domain.user.enums.OAuthProvider;
import com.example.toget.domain.user.repository.UserAccountRepository;
import com.example.toget.domain.user.repository.UserRepository;
import com.example.toget.domain.user.service.TokenIssuer;
import com.example.toget.global.enums.BankName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DevSetupService {

    private final UserRepository userRepository;
    private final UserAccountRepository userAccountRepository;
    private final FundingRepository fundingRepository;
    private final FundingMemberRepository fundingMemberRepository;
    private final ContributionBackgroundRepository contributionBackgroundRepository;
    private final FundingContributionRepository fundingContributionRepository;
    private final InvitationBackgroundRepository invitationBackgroundRepository;
    private final CharacterRepository characterRepository;
    private final InvitationCardRepository invitationCardRepository;
    private final FundingVisibilitySettingsRepository fundingVisibilitySettingsRepository;
    private final TokenIssuer tokenIssuer;

    @Transactional
    public DevSetupResponse setupTestData() {
        // 0. 마스터 데이터(축하 카드 배경, 초대장 배경, 캐릭터) DB 조회
        ContributionBackground defaultContribBg = getContributionBackground();
        InvitationBackground defaultInvBg = getInvitationBackground();
        CharacterEntity defaultCharacter = getCharacter();

        // 1. 유저 3명 생성 또는 조회 (개설자, 공동관리자, 참여자)
        User creator = getOrCreateDevUser("dev-creator-oauth-id", "creator@dev.com", "개설자", "개설자닉네임");
        User admin = getOrCreateDevUser("dev-admin-oauth-id", "admin@dev.com", "공동관리자", "공동관리자닉네임");
        User participant = getOrCreateDevUser("dev-participant-oauth-id", "participant@dev.com", "참여자", "참여자닉네임");

        // 2. 개설자 정산 계좌 세팅
        UserAccount creatorAccount = getOrCreateDevAccount(creator.getId());

        // 3. 각 유저별 Access Token + Refresh Token 발급
        TokenResponse creatorTokens = tokenIssuer.issue(creator);
        TokenResponse adminTokens = tokenIssuer.issue(admin);
        TokenResponse participantTokens = tokenIssuer.issue(participant);

        // 4. 5개 상태별 함께선물(TOGETHER_GIFT) 펀딩 생성 및 3명 멤버 자동 배정
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
                    .userAccountId(creatorAccount.getId())
                    .fundingType(FundingType.TOGETHER_GIFT)
                    .title("[" + status.name() + "] 함께선물 검증용 펀딩")
                    .recipientName("선물 수령인")
                    .anniversaryDate(LocalDate.now().plusDays(10))
                    .startDate(LocalDate.now().minusDays(1))
                    .endDate(LocalDate.now().plusDays(7))
                    .introduction("검증용 함께선물 펀딩입니다.")
                    .targetAmount(150000L)
                    .status(status)
                    .build();

            fundingRepository.save(funding);

            // 초대장 카드 및 공개 설정 자동 생성
            invitationCardRepository.save(InvitationCard.builder()
                    .fundingId(funding.getId())
                    .character(defaultCharacter)
                    .background(defaultInvBg)
                    .title("초대합니다!")
                    .content("함께선물 펀딩에 참여해 주세요.")
                    .url("https://toget.site/invitation/" + UUID.randomUUID())
                    .build());

            fundingVisibilitySettingsRepository.save(FundingVisibilitySettings.builder()
                    .fundingId(funding.getId())
                    .isProgressVisible(true)
                    .isParticipantCountVisible(true)
                    .isParticipantNameVisible(true)
                    .isMessageVisible(true)
                    .isCollectedAmountVisible(true)
                    .build());

            // 3명 계정을 개설자/공동관리자/참여자로 등록
            FundingMember creatorMember = FundingMember.createCreator(funding.getId(), creator.getId());
            FundingMember adminMember = FundingMember.createParticipant(funding.getId(), admin.getId());
            adminMember.promoteToAdmin();
            FundingMember participantMember = FundingMember.createParticipant(funding.getId(), participant.getId());

            // SETTLING 이상 상태인 경우 정산 확정 상태(amountDue & settlementStatus) 및 참여 내역 세팅
            if (status != FundingStatus.SELECTING) {
                long amountDue = 50000L;
                creatorMember.confirmSettlement(amountDue);
                adminMember.confirmSettlement(amountDue);
                participantMember.confirmSettlement(amountDue);

                if (status == FundingStatus.SETTLING) {
                    // 개설자는 입금 및 확인 완료(CONFIRMED), 공동관리자는 입금완료 신고(PAID), 참여자는 미입금(UNPAID)
                    creatorMember.requestPaymentConfirmation();
                    creatorMember.confirmPayment();
                    adminMember.requestPaymentConfirmation();

                    // 개설자/공동관리자의 입금 내역(FundingContribution) 추가 -> 모인금액 100,000원 (진행률 66.67%)
                    fundingContributionRepository.save(FundingContribution.createForLoggedInUser(
                            funding.getId(), defaultContribBg.getId(), creator.getId(), false, amountDue, "개설자 정산 입금", true
                    ));
                    fundingContributionRepository.save(FundingContribution.createForLoggedInUser(
                            funding.getId(), defaultContribBg.getId(), admin.getId(), false, amountDue, "공동관리자 정산 입금", true
                    ));
                } else {
                    // PURCHASING, DELIVERING, ENDED는 전원 정산 완료(CONFIRMED)
                    creatorMember.requestPaymentConfirmation();
                    creatorMember.confirmPayment();
                    adminMember.requestPaymentConfirmation();
                    adminMember.confirmPayment();
                    participantMember.requestPaymentConfirmation();
                    participantMember.confirmPayment();

                    fundingContributionRepository.save(FundingContribution.createForLoggedInUser(
                            funding.getId(), defaultContribBg.getId(), creator.getId(), false, amountDue, "정산 완료", true
                    ));
                    fundingContributionRepository.save(FundingContribution.createForLoggedInUser(
                            funding.getId(), defaultContribBg.getId(), admin.getId(), false, amountDue, "정산 완료", true
                    ));
                    fundingContributionRepository.save(FundingContribution.createForLoggedInUser(
                            funding.getId(), defaultContribBg.getId(), participant.getId(), false, amountDue, "정산 완료", true
                    ));
                }
            }

            fundingMemberRepository.save(creatorMember);
            fundingMemberRepository.save(adminMember);
            fundingMemberRepository.save(participantMember);

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

    private ContributionBackground getContributionBackground() {
        return contributionBackgroundRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new ContributionException(ContributionErrorCode.BACKGROUND_NOT_FOUND));
    }

    private InvitationBackground getInvitationBackground() {
        return invitationBackgroundRepository.findAllByDeletedAtIsNull().stream()
                .findFirst()
                .orElseThrow(() -> new FundingException(FundingErrorCode.INVITATION_BACKGROUND_NOT_FOUND));
    }

    private CharacterEntity getCharacter() {
        return characterRepository.findAllByDeletedAtIsNull().stream()
                .findFirst()
                .orElseThrow(() -> new FundingException(FundingErrorCode.CHARACTER_NOT_FOUND));
    }

    private UserAccount getOrCreateDevAccount(Long userId) {
        return userAccountRepository.findAllByUserIdOrderByIdAsc(userId).stream()
                .findFirst()
                .orElseGet(() -> userAccountRepository.save(
                        UserAccount.builder()
                                .userId(userId)
                                .bankName(BankName.KAKAO_BANK)
                                .accountOwner("개설자")
                                .account("3333-01-1234567")
                                .build()
                ));
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


