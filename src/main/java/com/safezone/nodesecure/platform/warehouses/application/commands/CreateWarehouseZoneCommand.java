package com.safezone.nodesecure.platform.warehouses.application.commands;

import com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects.OperatingShift;
import com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects.ZoneRiskLevel;

public record CreateWarehouseZoneCommand(Long companyId, Long warehouseId, String name, String code,
                                         ZoneRiskLevel riskLevel, OperatingShift operatingShift) {
}
