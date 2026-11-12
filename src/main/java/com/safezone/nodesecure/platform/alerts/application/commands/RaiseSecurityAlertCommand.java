package com.safezone.nodesecure.platform.alerts.application.commands;

import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertSeverity;
import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertType;

/**
 * Command for raising a security alert (US03, US05). Used by the discrepancy engine and the sensor integration.
 *
 * @param warehouseId     warehouse where the anomaly happened
 * @param zoneId          zone where the anomaly happened (optional)
 * @param physicalEventId physical event that originated the alert (optional)
 * @param alertType       kind of alert
 * @param severity        explicit severity, or {@code null} to use the default of the type
 * @param message         human-readable description
 */
public record RaiseSecurityAlertCommand(Long warehouseId, Long zoneId, Long physicalEventId, AlertType alertType,
                                        AlertSeverity severity, String message) {
    public RaiseSecurityAlertCommand {
        if (warehouseId == null || warehouseId <= 0)
            throw new IllegalArgumentException("alert.error.warehouseId.invalid");
        if (alertType == null)
            throw new IllegalArgumentException("alert.error.type.required");
    }
}
