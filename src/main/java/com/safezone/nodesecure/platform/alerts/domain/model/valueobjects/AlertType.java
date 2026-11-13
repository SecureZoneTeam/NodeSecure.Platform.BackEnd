package com.safezone.nodesecure.platform.alerts.domain.model.valueobjects;

import java.util.Locale;

/**
 * Kind of security alert raised by NodeSecure.
 * <ul>
 *     <li>{@link #DISCREPANCY}: a physical event has no inventory movement that justifies it (US03).</li>
 *     <li>{@link #UNAUTHORIZED_ACCESS}: physical access outside the authorized shift (US05).</li>
 *     <li>{@link #NODE_DISCONNECTION}: an IoT sensor node went offline.</li>
 * </ul>
 */
public enum AlertType {
    DISCREPANCY(AlertSeverity.MINOR),
    UNAUTHORIZED_ACCESS(AlertSeverity.CRITICAL),
    NODE_DISCONNECTION(AlertSeverity.MAJOR);

    private final AlertSeverity defaultSeverity;

    AlertType(AlertSeverity defaultSeverity) {
        this.defaultSeverity = defaultSeverity;
    }

    /**
     * @return severity used when an alert of this type is raised without an explicit severity
     */
    public AlertSeverity defaultSeverity() {
        return defaultSeverity;
    }

    /**
     * Parses an alert type from its textual name (case-insensitive).
     *
     * @param value raw type text
     * @return matching type
     * @throws IllegalArgumentException if the value is blank or unknown
     */
    public static AlertType fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("alert.error.type.required");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("alert.error.type.invalid");
        }
    }
}
