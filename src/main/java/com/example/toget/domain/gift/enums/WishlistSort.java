package com.example.toget.domain.gift.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum WishlistSort {
    LATEST,
    OLDEST;

    @JsonCreator
    public static WishlistSort from(String value) {
        if (value == null || value.isBlank()) {
            return LATEST;
        }
        String normalized = value.trim().toLowerCase();
        if ("oldest".equals(normalized) || "asc".equals(normalized)) {
            return OLDEST;
        }
        if ("latest".equals(normalized) || "desc".equals(normalized)) {
            return LATEST;
        }
        return Arrays.stream(values())
                .filter(sort -> sort.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid WishlistSort: " + value));
    }
}
