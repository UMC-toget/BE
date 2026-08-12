package com.example.toget.domain.gift.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum CategoryType {
    BIRTHDAY,
    GRADUATION,
    HOUSEWARMING;

    @JsonCreator
    public static CategoryType from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(categoryType -> categoryType.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid CategoryType: " + value));
    }
}
