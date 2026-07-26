package com.example.toget.domain.gift.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum ProductSort {
    LATEST,
    OLDEST,
    PRICE_ASC,
    PRICE_DESC;

    @JsonCreator
    public static ProductSort from(String value) {
        if (value == null || value.isBlank()) {
            return LATEST;
        }
        String normalized = value.trim().toLowerCase();
        if ("oldest".equals(normalized) || "asc".equals(normalized)) {
            return OLDEST;
        }
        if ("price_asc".equals(normalized)) {
            return PRICE_ASC;
        }
        if ("price_desc".equals(normalized)) {
            return PRICE_DESC;
        }
        if ("latest".equals(normalized) || "desc".equals(normalized)) {
            return LATEST;
        }
        return Arrays.stream(values())
                .filter(sort -> sort.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid ProductSort: " + value));
    }
}
