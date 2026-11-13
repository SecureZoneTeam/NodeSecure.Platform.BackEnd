package com.safezone.nodesecure.platform.alerts.domain.model.valueobjects;

import java.util.Locale;

/**
 * Lifecycle status of a security alert.
 * An alert is created {@link #OPEN} and can be closed once, either as {@link #RECONCILED}
 * or as {@link #FALSE_ALARM} (US07).
 */
public enum AlertStatus {
    OPEN,
    RECONCILED,
    FALSE_ALARM;

    /**
     * @return {@code true} when the alert was already closed
     */
    public boolean isResolved() {
        return this != OPEN;
    }

    /**
     * Parses a status from its textual name (case-insensitive).
     *
     * @param value raw status text
     * @return matching status
     * @throws IllegalArgumentException if the value is blank or unknown
     */
    public static AlertStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("alert.error.status.required");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("alert.error.status.invalid");
        }
    }
}
