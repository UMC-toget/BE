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

    @Column(length = 50)
    private String title;

    @Column(length = 50)
    private String receiver;

    @Column(name = "anniversary_date")
    private LocalDate anniversaryDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_image_url", columnDefinition = "TEXT")
    private String thumbnailImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "bank_name", length = 30)
    private BankName bankName;

    @Column(name = "bank_account", length = 50)
    private String bankAccount;

    @Column(name = "account_owner", length = 50)
    private String accountOwner;

    @Column(name = "card_title", columnDefinition = "TEXT")
    private String cardTitle;

    @Column(name = "card_content", columnDefinition = "TEXT")
    private String cardContent;
}

