package com.example.toget.domain.funding.dto.response;

/** 배경 색상 목록/단건 조회 응답 DTO */
public record ContributionBackgroundResponse(
        Long id,
        String name,
        String hexCode
) {}