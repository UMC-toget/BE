package com.example.toget.domain.funding.converter;

import com.example.toget.domain.funding.dto.response.ContributionBackgroundResponse;
import com.example.toget.domain.funding.entity.ContributionBackground;

public class ContributionBackgroundConverter {

    public static ContributionBackgroundResponse toResponse(ContributionBackground background) {
        return new ContributionBackgroundResponse(
                background.getId(),
                background.getName(),
                background.getHexCode(),
                background.getSolidColorHex()
        );
    }
}