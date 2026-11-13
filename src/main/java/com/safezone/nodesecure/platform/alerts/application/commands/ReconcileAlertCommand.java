package com.safezone.nodesecure.platform.alerts.application.commands;

/**
 * Command for closing an alert as reconciled once the physical event matches the inventory.
 *
 * @param alertId       alert to close
 * @param justification optional explanation
 * @param resolvedBy    username of the person who closes the alert
 */
public record ReconcileAlertCommand(Long alertId, String justification, String resolvedBy) {
    public ReconcileAlertCommand {
        if (alertId == null || alertId <= 0)
            throw new IllegalArgumentException("alert.error.id.invalid");
    }
}
