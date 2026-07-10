package com.example.toget.domain.workspace.entity;

import com.example.toget.global.enums.BankName;
import com.example.toget.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "funding_together_drafts")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FundingTogetherDraft extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "together_drafts_gift_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column
    private String title;

    @Column
    private String receiver;

    @Column(name = "anniversary_date")
    private LocalDate anniversaryDate;

    @Column
    private String description;

    @Column(name = "thumbnail_image_url")
    private String thumbnailImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "bank_name")
    private BankName bankName;

    @Column(name = "bank_account")
    private String bankAccount;

    @Column(name = "account_owner")
    private String accountOwner;

    @Column(name = "card_title")
    private String cardTitle;

    @Column(name = "card_content")
    private String cardContent;
}

