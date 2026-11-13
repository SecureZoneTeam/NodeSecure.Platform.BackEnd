package com.safezone.nodesecure.platform.sensors.domain.model.aggregates;

import com.safezone.nodesecure.platform.sensors.domain.model.valueobjects.PhysicalEventType;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class PhysicalEventTest {
    @Test void createsAnUnreconciledImmutablePhysicalRecord() {
        Instant occurredAt = Instant.parse("2026-11-11T12:00:00Z");
        PhysicalEvent event = PhysicalEvent.create(1L, 2L, 3L, PhysicalEventType.DOOR_OPENED, occurredAt);
        assertEquals(Long.valueOf(2L), event.getSensorNodeId());
        assertEquals(Long.valueOf(3L), event.getZoneId());
        assertEquals(PhysicalEventType.DOOR_OPENED, event.getEventType());
        assertEquals(occurredAt, event.getOccurredAt());
        assertFalse(event.isReconciled());
        assertEquals(1, event.pullDomainEvents().size());
    }
}
