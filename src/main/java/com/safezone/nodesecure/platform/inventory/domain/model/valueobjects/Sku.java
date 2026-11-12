package com.safezone.nodesecure.platform.inventory.domain.model.valueobjects;

import java.util.Locale;
import java.util.regex.Pattern;

public record Sku(String value) {
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 32;
    private static final Pattern ALLOWED_PATTERN = Pattern.compile("^[A-Z0-9]+(-[A-Z0-9]+)*$");
    private static final String INVALID_MESSAGE_KEY = "product.error.sku.invalid";

    public Sku {
        if (value == null) {
            throw new IllegalArgumentException(INVALID_MESSAGE_KEY);
        }
        value = value.trim().toUpperCase(Locale.ROOT);
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH || !ALLOWED_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(INVALID_MESSAGE_KEY);
        }
    }
}
