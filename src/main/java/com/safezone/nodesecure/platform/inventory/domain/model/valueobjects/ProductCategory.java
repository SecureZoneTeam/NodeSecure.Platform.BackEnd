package com.safezone.nodesecure.platform.inventory.domain.model.valueobjects;

import java.util.Locale;

public enum ProductCategory {
    NETWORKING,
    SECURITY,
    COMPUTING,
    ELECTRONICS,
    ACCESSORIES,
    OTHER;

    public static ProductCategory fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("product.error.category.required");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("product.error.category.invalid");
        }
    }
}
