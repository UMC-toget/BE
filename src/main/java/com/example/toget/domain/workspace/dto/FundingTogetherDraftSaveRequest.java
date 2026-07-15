package com.example.toget.domain.workspace.dto;

import java.time.LocalDate;

public record FundingTogetherDraftSaveRequest(
        Integer step,
        String title,
        String receiver,
        LocalDate anniversaryDate,
        LocalDate startDate,
        LocalDate endDate,
        String description,
        String thumbnailImageUrl,
        Long userAccountId,
        String cardTitle,
        String cardContent
) {}
