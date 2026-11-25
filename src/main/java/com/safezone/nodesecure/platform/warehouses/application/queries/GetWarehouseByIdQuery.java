package com.safezone.nodesecure.platform.warehouses.application.queries;

public record GetWarehouseByIdQuery(Long id, Long companyId, java.util.Set<Long> warehouseIds) {
}
