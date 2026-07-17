package com.example.toget.domain.funding.dto.request;

/** 배경 색상 생성/수정 공용 요청 DTO */
public record ContributionBackgroundRequest(
        String name,
        String hexCode
) {}