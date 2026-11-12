package com.safezone.nodesecure.platform.warehouses.interfaces.acl;

import com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects.WarehouseStatus;
import com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects.ZoneRiskLevel;

/** Read model exposed to other bounded contexts without leaking the Warehouse aggregate. */
public record WarehouseZoneConfiguration(Long warehouseId, Long zoneId, Long companyId,
                                          WarehouseStatus warehouseStatus, ZoneRiskLevel riskLevel,
                                          String openingShiftStart, String openingShiftEnd) {
}
