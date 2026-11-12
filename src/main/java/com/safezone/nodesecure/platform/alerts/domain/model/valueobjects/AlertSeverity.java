package com.safezone.nodesecure.platform.alerts.domain.model.valueobjects;

import java.util.Locale;

/**
 * Severity of a security alert.
 */
public enum AlertSeverity {
    MINOR,
    MAJOR,
    CRITICAL;

    /**
     * Parses a severity from its textual name (case-insensitive).
     *
     * @param value raw severity text
     * @return matching severity
     * @throws IllegalArgumentException if the value is blank or unknown
     */
    public static AlertSeverity fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("alert.error.severity.required");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("alert.error.severity.invalid");
        }
    }
}
