package com.safezone.nodesecure.platform.alerts.domain.model.events;

import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertStatus;
import com.safezone.nodesecure.platform.shared.domain.events.DomainEvent;

import java.time.Instant;

/**
 * Domain event emitted when a security alert is closed as reconciled or as a false alarm (US07).
 * The sensor integration context can listen to it to mark the related physical event as reconciled,
 * and the reporting context to record it in the traceability log.
 *
 * @param alertId         identifier of the closed alert
 * @param physicalEventId physical event related to the alert (may be {@code null})
 * @param status          final status: RECONCILED or FALSE_ALARM
 * @param resolvedBy      username of the person who closed the alert
 * @param occurredAt      instant the alert was closed
 */
public record SecurityAlertResolvedEvent(Long alertId, Long physicalEventId, AlertStatus status,
                                         String resolvedBy, Instant occurredAt) implements DomainEvent {
}
