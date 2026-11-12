package com.safezone.nodesecure.platform.warehouses.domain.model.entities;

import com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects.OperatingShift;
import com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects.ZoneRiskLevel;

import java.util.Objects;
import java.util.Locale;

/** Entity owned by the Warehouse aggregate. */
public final class WarehouseZone {

    private static final int MAX_NAME_LENGTH = 80;
    private static final int MAX_CODE_LENGTH = 30;

    private final Long id;
    private final Long warehouseId;
    private String name;
    private String code;
    private ZoneRiskLevel riskLevel;
    private OperatingShift operatingShift;

    private WarehouseZone(Long id, Long warehouseId, String name, String code,
                          ZoneRiskLevel riskLevel, OperatingShift operatingShift) {
        this.id = id;
        this.warehouseId = requirePositive(warehouseId, "warehouse.error.zone.warehouse.required");
        this.name = validateText(name, "warehouse.error.zone.name.required", MAX_NAME_LENGTH);
        this.code = validateText(code, "warehouse.error.zone.code.required", MAX_CODE_LENGTH).toUpperCase(Locale.ROOT);
        this.riskLevel = Objects.requireNonNull(riskLevel, "warehouse.error.zone.risk.invalid");
        this.operatingShift = Objects.requireNonNull(operatingShift, "warehouse.error.zone.shift.required");
    }

    public static WarehouseZone create(Long warehouseId, String name, String code,
                                       ZoneRiskLevel riskLevel, OperatingShift operatingShift) {
        return new WarehouseZone(null, warehouseId, name, code, riskLevel, operatingShift);
    }

    public static WarehouseZone restore(Long id, Long warehouseId, String name, String code,
                                        ZoneRiskLevel riskLevel, OperatingShift operatingShift) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("warehouse.error.zone.id.invalid");
        }
        return new WarehouseZone(id, warehouseId, name, code, riskLevel, operatingShift);
    }

    public void updateInformation(String name, String code, ZoneRiskLevel riskLevel, OperatingShift operatingShift) {
        this.name = validateText(name, "warehouse.error.zone.name.required", MAX_NAME_LENGTH);
        this.code = validateText(code, "warehouse.error.zone.code.required", MAX_CODE_LENGTH).toUpperCase(Locale.ROOT);
        this.riskLevel = Objects.requireNonNull(riskLevel, "warehouse.error.zone.risk.invalid");
        this.operatingShift = Objects.requireNonNull(operatingShift, "warehouse.error.zone.shift.required");
    }

    private static Long requirePositive(Long value, String messageKey) {
        if (value == null || value <= 0) throw new IllegalArgumentException(messageKey);
        return value;
    }

    private static String validateText(String value, String messageKey, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(messageKey);
        String normalized = value.trim();
        if (normalized.length() > maxLength) throw new IllegalArgumentException("warehouse.error.field.tooLong");
        return normalized;
    }

    public Long getId() { return id; }
    public Long getWarehouseId() { return warehouseId; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public ZoneRiskLevel getRiskLevel() { return riskLevel; }
    public OperatingShift getOperatingShift() { return operatingShift; }
}
