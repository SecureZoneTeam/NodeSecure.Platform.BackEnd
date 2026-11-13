package com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects;

public enum WarehouseStatus {
    ACTIVE,
    SUSPENDED;

    public static WarehouseStatus from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("warehouse.error.status.invalid");
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("warehouse.error.status.invalid");
        }
    }
}
