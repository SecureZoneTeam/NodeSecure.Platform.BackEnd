package com.safezone.nodesecure.platform.alerts.application.queries;

import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertSeverity;
import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertStatus;

/**
 * Query for listing security alerts, newest first. Every filter is optional.
 *
 * @param status      status filter
 * @param severity    severity filter
 * @param warehouseId warehouse filter
 * @param allowedWarehouseIds warehouses of the organization the user may see (tenant and scope filter)
 */
public record GetAllSecurityAlertsQuery(AlertStatus status, AlertSeverity severity, Long warehouseId,
                                        java.util.Set<Long> allowedWarehouseIds) {
    public GetAllSecurityAlertsQuery {
        if (warehouseId != null && warehouseId <= 0) {
            throw new IllegalArgumentException("alert.error.warehouseId.invalid");
        }
    }
}
