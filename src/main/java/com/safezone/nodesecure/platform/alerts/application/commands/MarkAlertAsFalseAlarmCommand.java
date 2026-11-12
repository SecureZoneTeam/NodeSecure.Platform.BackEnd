package com.safezone.nodesecure.platform.alerts.application.commands;

/**
 * Command for closing an alert as a false alarm because the movement was authorized (US07).
 *
 * @param alertId       alert to close
 * @param justification mandatory explanation
 * @param resolvedBy    username of the person who closes the alert
 */
public record MarkAlertAsFalseAlarmCommand(Long alertId, String justification, String resolvedBy) {
    public MarkAlertAsFalseAlarmCommand {
        if (alertId == null || alertId <= 0)
            throw new IllegalArgumentException("alert.error.id.invalid");
    }
}
