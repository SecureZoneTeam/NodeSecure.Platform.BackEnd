package com.safezone.nodesecure.platform.sensors.domain.model.events;

import com.safezone.nodesecure.platform.shared.domain.events.DomainEvent;
import com.safezone.nodesecure.platform.sensors.domain.model.valueobjects.SensorType;
import java.time.Instant;

/** Published when an ESP32 node is associated with a company's warehouse zone. */
public record SensorNodeRegisteredEvent(Long companyId, Long zoneId, String macAddress,
                                        SensorType sensorType, Instant occurredAt) implements DomainEvent {
}
