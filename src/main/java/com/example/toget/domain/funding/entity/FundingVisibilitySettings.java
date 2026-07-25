package com.example.toget.domain.funding.entity;

import com.example.toget.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 펀딩 공개 설정 — funding_visibility_settings 테이블 매핑.
 * fundings와 1:1 관계이며, 별도 테이블로 분리되어 있다 (Funding 생성 시 함께 생성).
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

    @Column(name = "funding_id", nullable = false)
    private Long fundingId;

    @Column(name = "is_progress_visible", nullable = false)
    private Boolean isProgressVisible = true;

    @Column(name = "is_participant_count_visible", nullable = false)
    private Boolean isParticipantCountVisible = true;

    @Column(name = "is_participant_name_visible", nullable = false)
    private Boolean isParticipantNameVisible = true;

    @Column(name = "is_message_visible", nullable = false)
    private Boolean isMessageVisible = true;

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