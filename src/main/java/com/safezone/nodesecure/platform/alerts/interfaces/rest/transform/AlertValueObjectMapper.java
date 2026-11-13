package com.safezone.nodesecure.platform.alerts.interfaces.rest.transform;

import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertSeverity;
import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertStatus;
import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertType;

/**
 * Interface layer translator converting string values from HTTP inputs into domain value objects.
 *
 * @since 1.0
 */
public class AlertValueObjectMapper {
    public static AlertType toAlertTypeFromString(String value) {
        return AlertType.fromValue(value);
    }

    /** @return the severity, or {@code null} when the value is null or blank (the type default applies) */
    public static AlertSeverity toOptionalSeverityFromString(String value) {
        return (value == null || value.isBlank()) ? null : AlertSeverity.fromValue(value);
    }

    /** @return the status, or {@code null} when the value is null or blank (no filter) */
    public static AlertStatus toOptionalStatusFromString(String value) {
        return (value == null || value.isBlank()) ? null : AlertStatus.fromValue(value);
    }
}
