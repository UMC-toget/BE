package com.example.toget.domain.workspace.entity;

import com.example.toget.global.enums.BankName;
import com.example.toget.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Column
    private String title;

    @Column(name = "anniversary_date")
    private LocalDate anniversaryDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column
    private String greeting;

    @Column(name = "thumbnail_url")
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

    @Column(name = "invitation_title")
    private String invitationTitle;

    @Column(name = "invitation_content")
    private String invitationContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "bank_name")
    private BankName bankName;

    @Column(name = "bank_account")
    private String bankAccount;

    @Column(name = "account_owner")
    private String accountOwner;
}

