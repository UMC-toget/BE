package com.example.toget.domain.gift.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum WishlistType {
    GIVE,    // 주고싶은 선물
    RECEIVE; // 받고싶은 선물

    @JsonCreator
    public static WishlistType from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(type -> type.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid WishlistType: " + value));
    }
}
