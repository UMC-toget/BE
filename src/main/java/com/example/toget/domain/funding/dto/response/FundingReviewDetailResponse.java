package com.example.toget.domain.funding.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record FundingReviewDetailResponse(
        Long fundingReviewId,
        String type,
        String title,
        String content,
        Long backgroundId,
        List<String> images,
        LocalDateTime createdAt
) {}