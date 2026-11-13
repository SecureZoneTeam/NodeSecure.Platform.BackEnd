package com.safezone.nodesecure.platform.inventory.domain.model.valueobjects;

import java.util.Locale;

public enum StockMovementType {
    ENTRY,
    EXIT,
    RECONCILIATION;

    public static StockMovementType fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("stock.error.type.required");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("stock.error.type.invalid");
        }
    }
}
