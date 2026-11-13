package com.safezone.nodesecure.platform.warehouses.interfaces.rest.resources;

import com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects.ZoneRiskLevel;

public record WarehouseZoneResource(Long id, Long warehouseId, String name, String code,
                                    ZoneRiskLevel riskLevel, String openingShiftStart, String openingShiftEnd) {
}
