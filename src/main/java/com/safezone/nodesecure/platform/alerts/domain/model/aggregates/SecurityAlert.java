package com.safezone.nodesecure.platform.alerts.domain.model.aggregates;

import com.safezone.nodesecure.platform.alerts.domain.exceptions.AlertAlreadyResolvedException;
import com.safezone.nodesecure.platform.alerts.domain.model.events.SecurityAlertRaisedEvent;
import com.safezone.nodesecure.platform.alerts.domain.model.events.SecurityAlertResolvedEvent;
import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertSeverity;
import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertStatus;
import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertType;
import com.safezone.nodesecure.platform.shared.domain.aggregates.AbstractDomainAggregateRoot;

import java.time.Instant;
import java.util.Objects;

/**
 * Security alert aggregate root. Pure domain: no ORM, Spring or transport dependencies.
 * <p>
 * An alert is raised {@link AlertStatus#OPEN} when the discrepancy engine or a sensor detects an anomaly
 * (US03, US05). It can be closed exactly once: as a false alarm with a mandatory justification (US07)
 * or as reconciled once the stock and the physical event match.
 */
public final class SecurityAlert extends AbstractDomainAggregateRoot {
    private static final int MESSAGE_MAX_LENGTH = 255;
    private static final int JUSTIFICATION_MAX_LENGTH = 300;
    private static final int RESOLVED_BY_MAX_LENGTH = 100;

    private final Long id;
    private final Long warehouseId;
    private final Long zoneId;
    private final Long physicalEventId;
    private final AlertType alertType;
    private final AlertSeverity severity;
    private AlertStatus status;
    private final String message;
    private final Instant raisedAt;
    private String justification;
    private Instant resolvedAt;
    private String resolvedBy;

    private SecurityAlert(Long id, Long warehouseId, Long zoneId, Long physicalEventId, AlertType alertType,
                          AlertSeverity severity, AlertStatus status, String message, Instant raisedAt,
                          String justification, Instant resolvedAt, String resolvedBy) {
        if (warehouseId == null || warehouseId <= 0) throw new IllegalArgumentException("alert.error.warehouseId.invalid");
        if (zoneId != null && zoneId <= 0) throw new IllegalArgumentException("alert.error.zoneId.invalid");
        if (physicalEventId != null && physicalEventId <= 0) throw new IllegalArgumentException("alert.error.physicalEventId.invalid");
        this.id = id;
        this.warehouseId = warehouseId;
        this.zoneId = zoneId;
        this.physicalEventId = physicalEventId;
        this.alertType = Objects.requireNonNull(alertType);
        this.severity = Objects.requireNonNull(severity);
        this.status = Objects.requireNonNull(status);
        this.message = normalizeMessage(message);
        this.raisedAt = Objects.requireNonNull(raisedAt);
        this.justification = justification;
        this.resolvedAt = resolvedAt;
        this.resolvedBy = resolvedBy;
    }

    /**
     * Raises a new open alert.
     *
     * @param severity explicit severity, or {@code null} to use the default of the alert type
     *                 (unauthorized access is CRITICAL, US05)
     */
    public static SecurityAlert raise(Long warehouseId, Long zoneId, Long physicalEventId, AlertType alertType,
                                      AlertSeverity severity, String message, Instant now) {
        if (alertType == null) throw new IllegalArgumentException("alert.error.type.required");
        var effectiveSeverity = severity != null ? severity : alertType.defaultSeverity();
        var alert = new SecurityAlert(null, warehouseId, zoneId, physicalEventId, alertType, effectiveSeverity,
                AlertStatus.OPEN, message, now, null, null, null);
        alert.registerEvent(new SecurityAlertRaisedEvent(warehouseId, zoneId, physicalEventId, alertType,
                effectiveSeverity, now));
        return alert;
    }

    /**
     * Rebuilds a persisted alert. Does not emit domain events.
     */
    public static SecurityAlert rehydrate(Long id, Long warehouseId, Long zoneId, Long physicalEventId,
                                          AlertType alertType, AlertSeverity severity, AlertStatus status,
                                          String message, Instant raisedAt, String justification,
                                          Instant resolvedAt, String resolvedBy) {
        if (id == null || id <= 0) throw new IllegalArgumentException("alert.error.id.invalid");
        return new SecurityAlert(id, warehouseId, zoneId, physicalEventId, alertType, severity, status, message,
                raisedAt, justification, resolvedAt, resolvedBy);
    }

    /**
     * Closes the alert as a false alarm because the movement was authorized (US07).
     * The justification is mandatory.
     *
     * @throws IllegalArgumentException      if the justification or the person are missing or too long
     * @throws AlertAlreadyResolvedException if the alert is not open
     */
    public void markAsFalseAlarm(String justification, String resolvedBy, Instant now) {
        var requiredJustification = requireJustification(justification);
        resolve(AlertStatus.FALSE_ALARM, requiredJustification, resolvedBy, now);
    }

    /**
     * Closes the alert as reconciled: the physical event now matches the inventory.
     * The justification is optional.
     *
     * @throws IllegalArgumentException      if the justification or the person are too long or missing
     * @throws AlertAlreadyResolvedException if the alert is not open
     */
    public void reconcile(String justification, String resolvedBy, Instant now) {
        resolve(AlertStatus.RECONCILED, optionalJustification(justification), resolvedBy, now);
    }

    private void resolve(AlertStatus newStatus, String normalizedJustification, String resolvedBy, Instant now) {
        var person = normalizeResolvedBy(resolvedBy);
        if (status.isResolved()) {
            throw new AlertAlreadyResolvedException(id, status);
        }
        this.status = newStatus;
        this.justification = normalizedJustification;
        this.resolvedBy = person;
        this.resolvedAt = Objects.requireNonNull(now);
        registerEvent(new SecurityAlertResolvedEvent(id, physicalEventId, newStatus, person, now));
    }

    /** @return {@code true} while the alert has not been closed */
    public boolean isOpen() {
        return status == AlertStatus.OPEN;
    }

    private static String normalizeMessage(String message) {
        if (message == null || message.isBlank()) throw new IllegalArgumentException("alert.error.message.notBlank");
        var trimmed = message.trim();
        if (trimmed.length() > MESSAGE_MAX_LENGTH) throw new IllegalArgumentException("alert.error.message.size");
        return trimmed;
    }

    private static String requireJustification(String justification) {
        if (justification == null || justification.isBlank()) throw new IllegalArgumentException("alert.error.justification.required");
        var trimmed = justification.trim();
        if (trimmed.length() > JUSTIFICATION_MAX_LENGTH) throw new IllegalArgumentException("alert.error.justification.size");
        return trimmed;
    }

    private static String optionalJustification(String justification) {
        if (justification == null || justification.isBlank()) return null;
        var trimmed = justification.trim();
        if (trimmed.length() > JUSTIFICATION_MAX_LENGTH) throw new IllegalArgumentException("alert.error.justification.size");
        return trimmed;
    }

    private static String normalizeResolvedBy(String resolvedBy) {
        if (resolvedBy == null || resolvedBy.isBlank()) throw new IllegalArgumentException("alert.error.resolvedBy.notBlank");
        var trimmed = resolvedBy.trim();
        if (trimmed.length() > RESOLVED_BY_MAX_LENGTH) throw new IllegalArgumentException("alert.error.resolvedBy.size");
        return trimmed;
    }

    public Long getId() {
        return id;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getPhysicalEventId() {
        return physicalEventId;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Instant getRaisedAt() {
        return raisedAt;
    }

    public String getJustification() {
        return justification;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }
}
