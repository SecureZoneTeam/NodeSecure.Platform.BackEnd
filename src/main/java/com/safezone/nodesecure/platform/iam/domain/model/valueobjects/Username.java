package com.safezone.nodesecure.platform.iam.domain.model.valueobjects;

import java.util.regex.Pattern;

public record Username(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9._-]{3,30}$");

    public Username {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("iam.error.username.notBlank");
        }
        value = value.trim().toLowerCase();
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("iam.error.username.invalid");
        }
    }
}
