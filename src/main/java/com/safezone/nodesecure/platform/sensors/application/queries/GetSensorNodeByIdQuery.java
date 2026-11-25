package com.safezone.nodesecure.platform.sensors.application.queries;

public record GetSensorNodeByIdQuery(Long id, Long companyId, java.util.Set<Long> warehouseIds) {
}
