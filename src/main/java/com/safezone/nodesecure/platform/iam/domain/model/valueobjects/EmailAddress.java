package com.safezone.nodesecure.platform.iam.domain.model.valueobjects;

import java.util.regex.Pattern;

public record EmailAddress(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MAX_LENGTH = 120;

    public EmailAddress {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("iam.error.email.notBlank");
        }
        value = value.trim().toLowerCase();
        if (value.length() > MAX_LENGTH || !PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("iam.error.email.invalid");
        }
    }
}
