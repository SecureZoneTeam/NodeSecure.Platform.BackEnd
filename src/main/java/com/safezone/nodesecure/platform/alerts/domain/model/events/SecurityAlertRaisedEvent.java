package com.safezone.nodesecure.platform.alerts.domain.model.events;

import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertSeverity;
import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertType;
import com.safezone.nodesecure.platform.shared.domain.events.DomainEvent;

import java.time.Instant;

/**
 * Domain event emitted when a security alert is raised (US03, US05).
 * A notification component (push or email, US06) can listen to it.
 *
 * @param warehouseId     warehouse where the alert happened
 * @param zoneId          zone where the alert happened (may be {@code null})
 * @param physicalEventId physical event that raised the alert (may be {@code null})
 * @param type            kind of alert
 * @param severity        severity of the alert
 * @param occurredAt      instant the alert was raised
 */
public record SecurityAlertRaisedEvent(Long warehouseId, Long zoneId, Long physicalEventId, AlertType type,
                                       AlertSeverity severity, Instant occurredAt) implements DomainEvent {
}
