package com.safezone.nodesecure.platform.iam.domain.model.valueobjects;

import java.util.Locale;

/** Account status. A suspended account cannot sign in and its tokens stop working immediately. */
public enum UserStatus {
    ACTIVE,
    SUSPENDED;

    public static UserStatus from(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        try {
            return UserStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("iam.error.status.invalid");
        }
    }
}
