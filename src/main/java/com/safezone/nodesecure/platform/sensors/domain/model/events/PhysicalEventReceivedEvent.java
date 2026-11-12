package com.safezone.nodesecure.platform.sensors.domain.model.events;

import com.safezone.nodesecure.platform.shared.domain.events.DomainEvent;
import com.safezone.nodesecure.platform.sensors.domain.model.valueobjects.PhysicalEventType;
import java.time.Instant;

/** Published after a physical event is persisted, for Alert/Traceability consumers. */
public record PhysicalEventReceivedEvent(Long companyId, Long sensorNodeId, Long zoneId,
                                          PhysicalEventType eventType, Instant occurredAt) implements DomainEvent {
}
