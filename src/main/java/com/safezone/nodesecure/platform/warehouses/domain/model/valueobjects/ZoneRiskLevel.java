package com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects;

public enum ZoneRiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static ZoneRiskLevel from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("warehouse.error.zone.risk.invalid");
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("warehouse.error.zone.risk.invalid");
        }
    }
}
