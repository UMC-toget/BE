package com.example.toget.domain.funding.entity;

import com.example.toget.domain.funding.enums.FundingStatus;
import com.example.toget.domain.funding.enums.FundingType;
import com.example.toget.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 펀딩(선물 준비) 엔티티
 *
 * [설계 포인트]
 *  - fundingType(MY_GIFT/TOGETHER_GIFT)에 따라 생성 시점의 초기 상태와
 *    계좌 필수 여부가 달라진다 — createMyGift/createTogetherGift 두 정적 팩토리로
 *    그 차이를 명시적으로 드러낸다 (Contribution과 동일한 설계 철학).
 *  - User/UserAccount와 마찬가지로 연관관계 없이 FK 값(userId, userAccountId)만 저장한다.
 *  - "확정 선물"을 추적하는 컬럼은 아직 스키마에 없기 때문에 추후 수정 필요.
 */
@Entity
@Table(name = "fundings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Funding extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 개최자 FK */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 선물 금액을 받을 계좌 FK.
     * MY_GIFT는 필수(본인이 직접 정산받으므로), TOGETHER_GIFT는 선택
     * (개최자가 아직 계좌를 등록하지 않고도 펀딩을 열 수 있게 허용).
     */
    @Column(name = "user_account_id")
    private Long userAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "funding_type", nullable = false, length = 20)
    private FundingType fundingType;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "recipient_name", nullable = false, length = 50)
    private String recipientName;

    @Column(name = "anniversary_date", nullable = false)
    private LocalDate anniversaryDate;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "introduction", columnDefinition = "TEXT")
    private String introduction;

    @Column(name = "thumbnail_image_url", length = 512)
    private String thumbnailImageUrl;

    /** 목표 금액 — 미입력 시 0 (목표 없이 자유롭게 모금하는 케이스 허용) */
    @Column(name = "target_amount", nullable = false)
    private Long targetAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FundingStatus status;

    @Builder
    private Funding(Long userId, Long userAccountId, FundingType fundingType, String title,
                    String recipientName, LocalDate anniversaryDate, LocalDate startDate,
                    LocalDate endDate, String introduction, String thumbnailImageUrl,
                    Long targetAmount, FundingStatus status) {
        this.userId = userId;
        this.userAccountId = userAccountId;
        this.fundingType = fundingType;
        this.title = title;
        this.recipientName = recipientName;
        this.anniversaryDate = anniversaryDate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.introduction = introduction;
        this.thumbnailImageUrl = thumbnailImageUrl;
        this.targetAmount = targetAmount != null ? targetAmount : 0L;
        this.status = status;
    }

    /**
     * 내 선물 만들기(MY_GIFT) 펀딩 생성.
     * 선물 후보 선정 단계가 없어 바로 ACTIVE로 시작하며, 본인이 직접 정산받으므로
     * userAccountId는 필수.
     */
    public static Funding createMyGift(Long userId, Long userAccountId, String title,
                                       String recipientName, LocalDate anniversaryDate,
                                       LocalDate startDate, LocalDate endDate,
                                       String introduction, String thumbnailImageUrl,
                                       Long targetAmount) {
        if (userAccountId == null) {
            // 예외처리 추가
        }
        return Funding.builder()
                .userId(userId)
                .userAccountId(userAccountId)
                .fundingType(FundingType.MY_GIFT)
                .title(title)
                .recipientName(recipientName)
                .anniversaryDate(anniversaryDate)
                .startDate(startDate)
                .endDate(endDate)
                .introduction(introduction)
                .thumbnailImageUrl(thumbnailImageUrl)
                .targetAmount(targetAmount)
                .status(FundingStatus.ACTIVE)
                .build();
    }

    /**
     * 함께 선물하기(TOGETHER_GIFT) 펀딩 생성.
     * 개최자가 최종 선물을 확정하기 전까지는 GIFT_SELECTING 상태이며,
     * 계좌는 아직 등록하지 않고도 펀딩을 열 수 있어 선택사항이다.
     */
    public static Funding createTogetherGift(Long userId, Long userAccountId, String title,
                                             String recipientName, LocalDate anniversaryDate,
                                             LocalDate startDate, LocalDate endDate,
                                             String introduction, String thumbnailImageUrl,
                                             Long targetAmount) {
        return Funding.builder()
                .userId(userId)
                .userAccountId(userAccountId)
                .fundingType(FundingType.TOGETHER_GIFT)
                .title(title)
                .recipientName(recipientName)
                .anniversaryDate(anniversaryDate)
                .startDate(startDate)
                .endDate(endDate)
                .introduction(introduction)
                .thumbnailImageUrl(thumbnailImageUrl)
                .targetAmount(targetAmount)
                .status(FundingStatus.GIFT_SELECTING)
                .build();
    }

    /**
     * 개최자가 최종 선물을 확정 — GIFT_SELECTING → ACTIVE (TOGETHER_GIFT 전용)
     *
     * TODO: 현재는 상태 전이만 수행하고 "어떤 선물이 확정됐는지"는 기록하지 않는다.
     * 여러 선물을 동시에 확정할 수 있으므로, 추후 수정 필요.
     */
    public void confirmGift() {
        if (this.status != FundingStatus.GIFT_SELECTING) {
        }
        this.status = FundingStatus.ACTIVE;
    }

    /** 펀딩 종료(조기 마감 또는 기간 만료) — ACTIVE → DELIVERED */
    public void close() {
        if (this.status != FundingStatus.ACTIVE) {
        }
        this.status = FundingStatus.ENDED;
    }

    /** 선물 리스트 갱신용 정산 계좌 등록/변경 — TOGETHER_GIFT가 나중에 계좌를 등록할 때 사용 */
    public void updateAccount(Long userAccountId) {
        this.userAccountId = userAccountId;
    }

    /** 정산 계좌 소유권 검사 — 서비스 계층에서 남의 펀딩 접근을 막을 때 사용 */
    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }
}