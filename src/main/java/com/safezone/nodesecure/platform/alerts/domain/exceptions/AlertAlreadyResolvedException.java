package com.safezone.nodesecure.platform.alerts.domain.exceptions;

import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertStatus;

/**
 * Raised when trying to close an alert that is no longer open.
 */
public class AlertAlreadyResolvedException extends RuntimeException {
    private final Long alertId;
    private final AlertStatus status;

    public AlertAlreadyResolvedException(Long alertId, AlertStatus status) {
        super("Security alert " + alertId + " is already " + status);
        this.alertId = alertId;
        this.status = status;
    }

    public Long getAlertId() {
        return alertId;
    }

    public AlertStatus getStatus() {
        return status;
    }
}
