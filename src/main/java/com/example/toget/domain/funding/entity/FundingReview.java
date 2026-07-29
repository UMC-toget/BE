package com.example.toget.domain.funding.entity;

import com.example.toget.domain.funding.enums.FundingReviewType;
import com.example.toget.domain.funding.exception.FundingException;
import com.example.toget.domain.funding.exception.code.FundingErrorCode;
import com.example.toget.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 펀딩 후기/소식/마음전하기 엔티티 — funding_reviews 테이블 매핑.
 *
 * [설계 포인트]
 *  - type으로 REVIEW(MY_GIFT 후기)/NEWS(전달 소식)/HEARTFELT(마음전하기)를 구분한다.
 *  - REVIEW는 title 없이 backgroundId를 사용, NEWS/HEARTFELT는 title 필수에 backgroundId 없음.
 *  - 개설자만 작성 가능. 펀딩당 타입별 최대 1개(UNIQUE(funding_id, type)).
 *  - 후기 전용 초대장은 이 콘텐츠와 1:1이라 별도 테이블 대신 컬럼으로 내장했다.
 *
 * [배경색 참조 — 두 필드가 서로 다른 테이블을 가리키니 혼동 주의]
 *  - backgroundId: 후기 카드 자체의 배경색. contribution_backgrounds 참조 (REVIEW만 사용).
 *  - invitationBackgroundId: 후기 보기 전 초대장의 배경색. invitation_backgrounds 참조.
 */
@Entity
@Table(
        name = "funding_reviews",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_funding_review_type", columnNames = {"funding_id", "type"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FundingReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "funding_id", nullable = false)
    private Long fundingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private FundingReviewType type;

    @Column(name = "title", length = 50)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 후기 카드 배경색 — contribution_backgrounds 참조. REVIEW만 값 존재 */
    @Column(name = "background_id")
    private Long backgroundId;

    @Column(name = "invitation_title", length = 15)
    private String invitationTitle;

    @Column(name = "invitation_content", length = 60)
    private String invitationContent;

    @Column(name = "invitation_character_id")
    private Long invitationCharacterId;

    /** 후기용 초대장 배경색 — invitation_backgrounds 참조 */
    @Column(name = "invitation_background_id")
    private Long invitationBackgroundId;

    @Builder
    private FundingReview(Long fundingId, FundingReviewType type, String title, String content,
                          Long backgroundId, String invitationTitle, String invitationContent,
                          Long invitationCharacterId, Long invitationBackgroundId) {
        validate(type, title, content);
        this.fundingId = fundingId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.backgroundId = backgroundId;
        this.invitationTitle = invitationTitle;
        this.invitationContent = invitationContent;
        this.invitationCharacterId = invitationCharacterId;
        this.invitationBackgroundId = invitationBackgroundId;
    }

    /** MY_GIFT 선물 후기 생성 — 제목 없음, 배경색 사용 */
    public static FundingReview createReview(Long fundingId, String content, Long backgroundId,
                                             String invitationTitle, String invitationContent,
                                             Long invitationCharacterId, Long invitationBackgroundId) {
        return FundingReview.builder()
                .fundingId(fundingId)
                .type(FundingReviewType.REVIEW)
                .content(content)
                .backgroundId(backgroundId)
                .invitationTitle(invitationTitle)
                .invitationContent(invitationContent)
                .invitationCharacterId(invitationCharacterId)
                .invitationBackgroundId(invitationBackgroundId)
                .build();
    }

    /** TOGETHER_GIFT 전달 소식 생성 — 제목 필수 */
    public static FundingReview createNews(Long fundingId, String title, String content,
                                           String invitationTitle, String invitationContent,
                                           Long invitationCharacterId, Long invitationBackgroundId) {
        return FundingReview.builder()
                .fundingId(fundingId)
                .type(FundingReviewType.NEWS)
                .title(title)
                .content(content)
                .invitationTitle(invitationTitle)
                .invitationContent(invitationContent)
                .invitationCharacterId(invitationCharacterId)
                .invitationBackgroundId(invitationBackgroundId)
                .build();
    }

    /** TOGETHER_GIFT 마음 전하기 생성 — 제목 필수 */
    public static FundingReview createHeartfelt(Long fundingId, String title, String content,
                                                String invitationTitle, String invitationContent,
                                                Long invitationCharacterId, Long invitationBackgroundId) {
        return FundingReview.builder()
                .fundingId(fundingId)
                .type(FundingReviewType.HEARTFELT)
                .title(title)
                .content(content)
                .invitationTitle(invitationTitle)
                .invitationContent(invitationContent)
                .invitationCharacterId(invitationCharacterId)
                .invitationBackgroundId(invitationBackgroundId)
                .build();
    }

    public void update(String title, String content, Long backgroundId,
                       String invitationTitle, String invitationContent,
                       Long invitationCharacterId, Long invitationBackgroundId) {
        validate(this.type, title, content);
        this.title = title;
        this.content = content;
        this.backgroundId = backgroundId;
        this.invitationTitle = invitationTitle;
        this.invitationContent = invitationContent;
        this.invitationCharacterId = invitationCharacterId;
        this.invitationBackgroundId = invitationBackgroundId;
    }

    private static void validate(FundingReviewType type, String title, String content) {
        if (content == null || content.isBlank()) {
            throw new FundingException(FundingErrorCode.REVIEW_CONTENT_REQUIRED);
        }
        if (type != FundingReviewType.REVIEW && (title == null || title.isBlank())) {
            throw new FundingException(FundingErrorCode.REVIEW_TITLE_REQUIRED);
        }
    }
}