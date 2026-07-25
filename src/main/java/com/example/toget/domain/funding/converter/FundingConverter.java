package com.example.toget.domain.funding.converter;

import com.example.toget.domain.funding.dto.MyFundingListResponse;
import com.example.toget.domain.funding.dto.MyFundingListResponse.MyFundingSummary;
import com.example.toget.domain.funding.dto.response.*;
import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.funding.entity.FundingContribution;
import com.example.toget.domain.funding.entity.FundingMember;
import com.example.toget.domain.funding.enums.FundingRole;
import com.example.toget.domain.user.entity.User;
import com.example.toget.domain.funding.entity.Funding;
import com.example.toget.domain.user.entity.UserAccount;

import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Map;

/**
 * Funding 엔티티 → 응답 DTO 변환 전담 클래스.
 * collectedAmount는 엔티티에 없는 계산 값이라 합산 맵을 함께 받아 조립한다.
 * 상태가 없으므로 static 메서드로만 구성 (private 생성자로 인스턴스화 차단) — UserConverter와 동일한 규칙.
 */
public class FundingConverter {

    private static final int PERCENT = 100;

    private FundingConverter() {
    }

    public static MyFundingListResponse toMyFundingListResponse(Slice<Funding> fundings,
                                                                Map<Long, Long> collectedAmounts) {
        List<MyFundingSummary> items = fundings.getContent().stream()
                .map(funding -> toMyFundingSummary(
                        funding,
                        collectedAmounts.getOrDefault(funding.getId(), 0L) // 참여금 없는 펀딩은 0
                ))
                .toList();
        return new MyFundingListResponse(
                items,
                fundings.getNumber(),  // 현재 페이지 번호
                fundings.getSize(),    // 요청한 페이지 크기
                fundings.hasNext()
        );
    }

    private static MyFundingSummary toMyFundingSummary(Funding funding, Long collectedAmount) {
        return new MyFundingSummary(
                funding.getId(),
                funding.getFundingType().name(),
                funding.getTitle(),
                funding.getRecipientName(),
                funding.getTargetAmount(),
                collectedAmount,
                calculateProgressRate(collectedAmount, funding.getTargetAmount()),
                funding.getStatus().name(),
                funding.getEndDate(),
                funding.getThumbnailImageUrl(),
                funding.getCreatedAt()
        );
    }

    /**
     * 소수점 내림(정수%), 100 초과 허용(초과 달성 그대로 노출)
     * targetAmount는 0 허용 -> 0 나눗셈을 방어
     */
    private static int calculateProgressRate(Long collectedAmount, Long targetAmount) {
        if (targetAmount == null || targetAmount == 0) {
            return 0; // targetAmount는 0허용
        }
        return (int) (collectedAmount * PERCENT / targetAmount);
    }

    public static FundingMemberManagementResponse.MemberInfo toMemberInfo(FundingMember member, Map<Long, User> userMap) {
        User user = userMap.get(member.getUserId());
        return new FundingMemberManagementResponse.MemberInfo(
                member.getId(), member.getUserId(), user.getName(), user.getProfileImageUrl(), member.getRole().name()
        );
    }

    public static FundingMemberManagementResponse toMemberManagementResponse(List<FundingMember> members, Map<Long, User> userMap) {
        List<FundingMemberManagementResponse.MemberInfo> admins = members.stream()
                .filter(m -> m.getRole() == FundingRole.CREATOR || m.getRole() == FundingRole.ADMIN)
                .map(m -> toMemberInfo(m, userMap))
                .toList();

        List<FundingMemberManagementResponse.MemberInfo> participants = members.stream()
                .filter(m -> m.getRole() == FundingRole.PARTICIPANT)
                .map(m -> toMemberInfo(m, userMap))
                .toList();

        return new FundingMemberManagementResponse(
                members.size(), admins.size(), participants.size(), admins, participants
        );
    }

    public static FundingSettlementListResponse.SettlementInfo toSettlementInfo(FundingMember member, Map<Long, User> userMap) {
        User user = userMap.get(member.getUserId());
        return new FundingSettlementListResponse.SettlementInfo(
                member.getId(), member.getUserId(), user.getName(), user.getProfileImageUrl(),
                member.getAmountDue(), member.getSettlementStatus().name()
        );
    }

    public static FundingSettlementListResponse toSettlementListResponse(List<FundingMember> settlementMembers, Map<Long, User> userMap) {
        long totalAmount = settlementMembers.stream()
                .mapToLong(FundingMember::getAmountDue)
                .sum();

        List<FundingSettlementListResponse.SettlementInfo> settlements = settlementMembers.stream()
                .map(m -> toSettlementInfo(m, userMap))
                .toList();

        return new FundingSettlementListResponse(settlementMembers.size(), totalAmount, settlements);
    }

    public static FundingCreateResponse toCreateResponse(Funding funding) {
        return new FundingCreateResponse(funding.getId());
    }

    public static FundingBasicInfoResponse toBasicInfoResponse(Funding funding) {
        return new FundingBasicInfoResponse(
                funding.getId(),
                funding.getTitle(),
                funding.getAnniversaryDate(),
                funding.getStartDate(),
                funding.getEndDate(),
                funding.getIntroduction(),
                funding.getThumbnailImageUrl()
        );
    }

    public static FundingAccountResponse toAccountResponse(UserAccount account) {
        return new FundingAccountResponse(
                account.getId(),
                account.getBankName().name(),
                account.getAccount(),
                account.getAccountOwner()
        );
    }

    public static FundingAccountUpdateResponse toAccountUpdateResponse(Funding funding) {
        return new FundingAccountUpdateResponse(funding.getId(), funding.getUserAccountId());
    }

    public static FundingContributionAmountUpdateResponse toContributionAmountUpdateResponse(FundingContribution contribution) {
        return new FundingContributionAmountUpdateResponse(contribution.getId(), contribution.getAmount());
    }

}
