package com.example.toget.domain.funding.entity;

import com.example.toget.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 펀딩 공개 설정 — funding_visibility_settings 테이블 매핑.
 * fundings와 1:1 관계이며, 별도 테이블로 분리되어 있다.
 *
 * [설계 포인트]
 *  - MY_GIFT 펀딩에만 생성된다. TOGETHER_GIFT는 초대된 멤버끼리 서로의 참여·정산 현황을
 *    모두 봐야 하는 구조라 가릴 대상이 없어 설정 행 자체를 만들지 않는다.
 *  - 각 필드는 "외부 방문자에게 보여줄지"를 뜻하며, 개설자 본인 대시보드는 이 설정과 무관하게
 *    항상 원본 값을 노출한다.
 *
 * [아직 구현 안 함]
 *  - 이 설정을 실제로 반영하는 외부 방문자용 조회 API가 아직 없다.
 *    현재는 값을 저장하고 개설자 대시보드에서 되읽는 것까지만 동작한다.
 */
@Entity
@Table(name = "funding_visibility_settings")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FundingVisibilitySettings extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "funding_visibility_setting_id")
    private Long id;

    // 이 설정이 속한 펀딩의 ID (FK -> fundings.funding_id)
    @Column(name = "funding_id", nullable = false)
    private Long fundingId;

    // 진행률(목표 대비 달성 퍼센트) 노출 여부
    @Column(name = "is_progress_visible", nullable = false)
    private Boolean isProgressVisible = true;

    // 참여자 수 노출 여부
    @Column(name = "is_participant_count_visible", nullable = false)
    private Boolean isParticipantCountVisible = true;

    // 참여자 이름 노출 여부 (익명 참여 여부와는 별개로 전체를 가리는 스위치)
    @Column(name = "is_participant_name_visible", nullable = false)
    private Boolean isParticipantNameVisible = true;

    // 축하 메시지(롤링페이퍼) 노출 여부
    @Column(name = "is_message_visible", nullable = false)
    private Boolean isMessageVisible = true;

    // 모금액 노출 여부
    @Column(name = "is_collected_amount_visible", nullable = false)
    private Boolean isCollectedAmountVisible = true;

    @Builder
    private FundingVisibilitySettings(Long fundingId, Boolean isProgressVisible,
                                      Boolean isParticipantCountVisible, Boolean isParticipantNameVisible,
                                      Boolean isMessageVisible, Boolean isCollectedAmountVisible) {
        this.fundingId = fundingId;
        this.isProgressVisible = isProgressVisible != null ? isProgressVisible : true;
        this.isParticipantCountVisible = isParticipantCountVisible != null ? isParticipantCountVisible : true;
        this.isParticipantNameVisible = isParticipantNameVisible != null ? isParticipantNameVisible : true;
        this.isMessageVisible = isMessageVisible != null ? isMessageVisible : true;
        this.isCollectedAmountVisible = isCollectedAmountVisible != null ? isCollectedAmountVisible : true;
    }

    /**
     * 기본값(전부 공개)으로 빈 설정 행을 생성한다.
     * 실제 값은 호출부에서 이어서 update()로 채우는 것을 전제로 한다.
     * create()에 Boolean 5개를 위치 인자로 넘기지 않아도 되므로 순서 실수를 원천 차단한다.
     */
    public static FundingVisibilitySettings createDefault(Long fundingId) {
        return FundingVisibilitySettings.builder()
                .fundingId(fundingId)
                .build();
    }

    public static FundingVisibilitySettings create(Long fundingId, Boolean isProgressVisible,
                                                   Boolean isParticipantCountVisible, Boolean isParticipantNameVisible,
                                                   Boolean isMessageVisible, Boolean isCollectedAmountVisible) {
        return FundingVisibilitySettings.builder()
                .fundingId(fundingId)
                .isProgressVisible(isProgressVisible)
                .isParticipantCountVisible(isParticipantCountVisible)
                .isParticipantNameVisible(isParticipantNameVisible)
                .isMessageVisible(isMessageVisible)
                .isCollectedAmountVisible(isCollectedAmountVisible)
                .build();
    }

    public void update(Boolean isProgressVisible, Boolean isParticipantCountVisible,
                       Boolean isParticipantNameVisible, Boolean isMessageVisible,
                       Boolean isCollectedAmountVisible) {
        this.isProgressVisible = isProgressVisible;
        this.isParticipantCountVisible = isParticipantCountVisible;
        this.isParticipantNameVisible = isParticipantNameVisible;
        this.isMessageVisible = isMessageVisible;
        this.isCollectedAmountVisible = isCollectedAmountVisible;
    }
}