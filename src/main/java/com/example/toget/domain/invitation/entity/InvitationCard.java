package com.example.toget.domain.invitation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 펀딩에 첨부되는 초대장 카드 엔티티
 *
 * [설계 포인트]
 *  - fundingId: Funding 엔티티와의 결합도를 낮추고 Funding 정보가 필요한 시점에만 명시적으로 조회하기 위해
 *    @OneToOne 연관관계 대신 FK 값(Long)만 저장한다.
 *  - character/background: 같은 invitation 도메인 내부 참조라 @ManyToOne(LAZY) 연관관계를 사용한다.
 *  - url: 초대장 공유 링크.
 *    URL 생성 방식(UUID, 슬러그, id 기반 등)은 아직 확정되지 않아 현재는 Builder를 통해 외부에서 값을 주입받는다.
 */
@Entity
// funding_id에 UNIQUE 제약 추가 — 펀딩:초대장은 1:1 관계.
@Table(name = "invitation_cards", uniqueConstraints = {
        @UniqueConstraint(name = "uk_invitation_cards_funding", columnNames = {"funding_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvitationCard {

    @Id
    // PK 생성 -> MySQL AUTO_INCREMENT에 ID 생성을 위임
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // 이 초대장이 속한 funding의 ID
    @Column(name = "funding_id", nullable = false)
    private Long fundingId;

    // 카드에 표시할 캐릭터
    // characters 테이블과 단방향 @ManyToOne(LAZY) 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private CharacterEntity character;

    // 카드 배경 색상
    // invitation_backgrounds 테이블과 단방향 @ManyToOne(LAZY) 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "background_id", nullable = false)
    private InvitationBackground background;

    // 초대장 제목
    // TODO: 초대장 수정 API 구현 시 InvitationCardRequest에 @Size(max = 15) 추가 필요
    @Column(name = "title", nullable = false, length = 15)
    private String title;

    // 초대장 본문 내용
    // TODO: 초대장 수정 API 구현 시 InvitationCardRequest에 @Size(max = 60) 추가 필요
    @Column(name = "content", nullable = false, length = 60)
    private String content;

    // 초대장 공유 링크
    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;

    @Builder
    private InvitationCard(Long fundingId, CharacterEntity character, InvitationBackground background,
                            String title, String content, String url) {
        this.fundingId = fundingId;
        this.character = character;
        this.background = background;
        this.title = title;
        this.content = content;
        this.url = url;
    }
}
