package com.example.toget.domain.funding.entity;

import com.example.toget.domain.funding.enums.FundingStatus;
import com.example.toget.domain.funding.enums.FundingType;
import com.example.toget.domain.funding.exception.code.FundingErrorCode;
import com.example.toget.global.apiPayload.exception.ProjectException;
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
 *    그 차이를 명시적으로 드러낸다.
 *  - "확정 선물"은 FundingGift.status(SELECTED)로 추적하고, Funding은 진행 단계(status)만 갖는다.
 *  - TOGETHER_GIFT의 정산 인원/금액/입금상태는 FundingMember가 담당한다 (Funding은 관여하지 않음).
 *  - User/UserAccount와 마찬가지로 연관관계 없이 FK 값(userId, userAccountId)만 저장한다.
 *  - SETTLING 진입 이후에는 endDate와 무관하게 정산이 계속 진행된다.
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

    /**
     * 개최자 FK
     */
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
        validatePeriod(startDate, endDate);
        validateTargetAmount(targetAmount);

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
        this.targetAmount = targetAmount;
        this.status = status;
    }

    private static void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new ProjectException(FundingErrorCode.INVALID_FUNDING_PERIOD);
        }
    }

    private static void validateTargetAmount(Long targetAmount) {
        if (targetAmount == null || targetAmount < 0) {
            throw new ProjectException(FundingErrorCode.INVALID_TARGET_AMOUNT);
        }
    }

    /**
     * 내 선물 만들기(MY_GIFT) 펀딩 생성.
     * 선물 후보 선정 단계가 없어 바로 SETTLING으로 시작하며, 본인이 직접 정산받으므로
     * userAccountId는 필수.
     */
    public static Funding createMyGift(Long userId, Long userAccountId, String title,
                                       String recipientName, LocalDate anniversaryDate,
                                       LocalDate startDate, LocalDate endDate,
                                       String introduction, String thumbnailImageUrl,
                                       Long targetAmount) {
        if (userAccountId == null) {
            throw new ProjectException(FundingErrorCode.ACCOUNT_REQUIRED_FOR_MY_GIFT);
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
                .status(FundingStatus.SETTLING)
                .build();
    }

    /**
     * 함께 선물하기(TOGETHER_GIFT) 펀딩 생성.
     * 개최자가 정산(선물 확정 + 정산인원 확정 + 금액 확정)을 완료하기 전까지는
     * SELECTING 상태이며, 계좌는 아직 등록하지 않고도 펀딩을 열 수 있어 선택사항이다.
     * <p>
     * 생성 시점에는 FundingMember(개설자=CREATOR) row가 함께 생성되어야 한다.
     * 이 메서드는 Funding 자체만 생성하므로, 호출부(서비스 계층)에서
     * FundingMember.createCreator(...)를 이어서 호출해 트랜잭션으로 묶어야 한다.
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
                .status(FundingStatus.SELECTING)
                .build();
    }

    /**
     * 정산 확정(4단계 "정산 시작하기") — SELECTING → SETTLING (TOGETHER_GIFT 전용)
     * <p>
     * 프론트에서 선물 확정/정산인원 확정/금액 확정을 한 번에 모아 저장하는 원자적 액션이므로,
     * 이 메서드 호출 시점에 FundingGift.select(...), FundingMember.confirmSettlement(...)가
     * 같은 트랜잭션 안에서 함께 처리되어야 한다.
     */
    public void confirmSettlement(Long targetAmount) {
        if (this.status != FundingStatus.SELECTING) {
            throw new ProjectException(FundingErrorCode.INVALID_FUNDING_STATUS_TRANSITION);
        }
        validateTargetAmount(targetAmount);
        this.targetAmount = targetAmount;
        this.status = FundingStatus.SETTLING;
    }

    /**
     * 펀딩 종료 처리 — SELECTING 또는 SETTLING → ENDED
     * 최종 선물이 확정된 채 종료(준비완료)든, 미확정 상태로 개설자가 종료를 선택했든
     * 동일하게 ENDED로 귀결된다. "실제로 선물이 전달되었는지"는 FundingGift 조회로 구분한다.
     */
    public void complete() {
        if (this.status == FundingStatus.ENDED) {
            throw new ProjectException(FundingErrorCode.INVALID_FUNDING_STATUS_TRANSITION);
        }
        this.status = FundingStatus.ENDED;
    }


    /**
     *  종료일 변경 — T종료 결정 필요 상태에서 개설자가 "종료일 변경하기" 선택 시 사용
     */
    public void updateEndDate(LocalDate newEndDate) {
        validatePeriod(this.startDate, newEndDate);
        this.endDate = newEndDate;
    }

    /**
     * 선물 리스트 갱신용 정산 계좌 등록/변경 — TOGETHER_GIFT가 나중에 계좌를 등록할 때 사용
     */
    public void updateAccount(Long userAccountId) {

        this.userAccountId = userAccountId;
    }

    /**
     * 정산 계좌 소유권 검사 — 서비스 계층에서 남의 펀딩 접근을 막을 때 사용
     */
    public boolean isOwnedBy(Long userId) {

        return this.userId.equals(userId);
    }

    private boolean isExpired() {

        return LocalDate.now().isAfter(this.endDate);
    }


    /**
     * SELECTING 상태에서 기간이 지나 개설자의 종료 결정이 필요한지
     */
    public boolean needsEndDecision() {
        return this.status == FundingStatus.SELECTING && isExpired();
    }
}