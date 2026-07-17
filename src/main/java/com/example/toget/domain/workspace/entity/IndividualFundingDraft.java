package com.example.toget.domain.workspace.entity;

import com.example.toget.domain.invitation.entity.CharacterEntity;
import com.example.toget.domain.invitation.entity.InvitationBackground;
import com.example.toget.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "individual_funding_drafts")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class IndividualFundingDraft extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "my_drafts_gift_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column
    private Integer step;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(name = "anniversary_date")
    private LocalDate anniversaryDate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String greeting;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "is_progress_public")
    @Builder.Default
    private Boolean isProgressPublic = true;

    @Column(name = "is_amount_public")
    @Builder.Default
    private Boolean isAmountPublic = true;

    @Column(name = "is_participant_count_public")
    @Builder.Default
    private Boolean isParticipantCountPublic = true;

    @Column(name = "is_participant_name_public")
    @Builder.Default
    private Boolean isParticipantNamePublic = true;

    @Column(name = "is_message_public")
    @Builder.Default
    private Boolean isMessagePublic = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitation_character_id", nullable = true)
    private CharacterEntity invitationCharacter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitation_background_id", nullable = true)
    private InvitationBackground invitationBackground;

    @Column(name = "invitation_title", columnDefinition = "TEXT")
    private String invitationTitle;

    @Column(name = "invitation_content", columnDefinition = "TEXT")
    private String invitationContent;

    @Column(name = "user_account_id")
    private Long userAccountId;

    public void update(
            Integer step,
            String title,
            LocalDate anniversaryDate,
            LocalDate startDate,
            LocalDate endDate,
            String greeting,
            String thumbnailUrl,
            Long userAccountId,
            Boolean isProgressPublic,
            Boolean isAmountPublic,
            Boolean isParticipantCountPublic,
            Boolean isParticipantNamePublic,
            Boolean isMessagePublic,
            CharacterEntity invitationCharacter,
            InvitationBackground invitationBackground,
            String invitationTitle,
            String invitationContent
    ) {
        this.step = step;
        this.title = title;
        this.anniversaryDate = anniversaryDate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.greeting = greeting;
        this.thumbnailUrl = thumbnailUrl;
        this.userAccountId = userAccountId;
        this.isProgressPublic = isProgressPublic;
        this.isAmountPublic = isAmountPublic;
        this.isParticipantCountPublic = isParticipantCountPublic;
        this.isParticipantNamePublic = isParticipantNamePublic;
        this.isMessagePublic = isMessagePublic;
        this.invitationCharacter = invitationCharacter;
        this.invitationBackground = invitationBackground;
        this.invitationTitle = invitationTitle;
        this.invitationContent = invitationContent;
    }
}

