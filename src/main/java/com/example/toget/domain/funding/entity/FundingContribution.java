package com.example.toget.domain.funding.entity;

import com.example.toget.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 펀딩 후원(참여) 엔티티
 *
 * [설계 포인트]
 *  - "축하메시지 작성 + 계좌이체 신고"가 동시에 일어나는 하나의 행위 기록이다.
 *    환불 개념이 없고 결제 단계 UI 노출도 없으므로 별도 status 컬럼을 두지 않는다
 *    (row가 존재하는 것 자체가 "신고 완료"를 의미).
 *  - MY_GIFT: 비회원(guest) 후원 가능 — userId는 null, guestName 사용.
 *  - TOGETHER_GIFT: 로그인 멤버 전용 — userId 채워짐, guestName은 null.
 *    실제 정산 확정 금액/입금상태의 진실(source of truth)은 FundingMember가 갖고,
 *    이 테이블은 어디까지나 "신고 이벤트 로그"다.
 *  - userId/guestName 상호배타는 DB 제약이 아닌 애플리케이션 검증으로 보장한다.
 */
@Entity
@Table(name = "funding_contributions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FundingContribution extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "funding_id", nullable = false)
    private Long fundingId;

    @Column(name = "background_id", nullable = false)
    private Long backgroundId;

    /** 로그인 멤버의 후원인 경우. MY_GIFT의 비회원 후원은 null */
    @Column(name = "user_id")
    private Long userId;

    /** 비회원 후원자 이름. 로그인 멤버 후원은 null */
    @Column(name = "guest_name", length = 50)
    private String guestName;

    @Column(name = "is_anonymous", nullable = false)
    private Boolean isAnonymous;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_message_visible", nullable = false)
    private Boolean isMessageVisible;

    @Builder
    private FundingContribution(Long fundingId, Long backgroundId, Long userId, String guestName,
                                Boolean isAnonymous, Long amount, String content,
                                Boolean isMessageVisible) {
        this.fundingId = fundingId;
        this.backgroundId = backgroundId;
        this.userId = userId;
        this.guestName = guestName;
        this.isAnonymous = isAnonymous != null ? isAnonymous : false;
        this.amount = amount;
        this.content = content;
        this.isMessageVisible = isMessageVisible != null ? isMessageVisible : true;
    }

    /** 비회원(guest) 후원 생성 — MY_GIFT에서 로그인 없이 참여하는 경우 */
    public static FundingContribution createForGuest(Long fundingId, Long backgroundId,
                                                     String guestName, Boolean isAnonymous,
                                                     Long amount, String content) {
        return FundingContribution.builder()
                .fundingId(fundingId)
                .backgroundId(backgroundId)
                .guestName(guestName)
                .isAnonymous(isAnonymous)
                .amount(amount)
                .content(content)
                .build();
    }

    /** 로그인 유저 후원 생성 — MY_GIFT/TOGETHER_GIFT 공통 */
    public static FundingContribution createForLoggedInUser(Long fundingId, Long backgroundId,
                                                            Long userId, Boolean isAnonymous,
                                                            Long amount, String content, Boolean isMessageVisible) {
        return FundingContribution.builder()
                .fundingId(fundingId)
                .backgroundId(backgroundId)
                .userId(userId)
                .isAnonymous(isAnonymous)
                .amount(amount)
                .content(content)
                .isMessageVisible(isMessageVisible)
                .build();
    }

    public void updateMessageVisibility(boolean isMessageVisible) {
        this.isMessageVisible = isMessageVisible;
    }
}