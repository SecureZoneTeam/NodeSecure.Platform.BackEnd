package com.safezone.nodesecure.platform.warehouses.application.queries;

public record GetWarehouseZoneByIdQuery(Long zoneId, Long companyId, java.util.Set<Long> warehouseIds) {
}
