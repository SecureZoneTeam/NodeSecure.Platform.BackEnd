package com.safezone.nodesecure.platform.sensors.interfaces.rest.resources;

import com.safezone.nodesecure.platform.sensors.domain.model.valueobjects.PhysicalEventType;
import java.time.Instant;

public record PhysicalEventResource(Long id, Long sensorNodeId, Long zoneId, PhysicalEventType eventType,
                                    Instant occurredAt, boolean reconciled) {
}
