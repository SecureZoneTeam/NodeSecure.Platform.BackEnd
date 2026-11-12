package com.safezone.nodesecure.platform.sensors.domain.model.aggregates;

import com.safezone.nodesecure.platform.shared.domain.aggregates.AbstractDomainAggregateRoot;
import com.safezone.nodesecure.platform.sensors.domain.model.events.PhysicalEventReceivedEvent;
import com.safezone.nodesecure.platform.sensors.domain.model.valueobjects.PhysicalEventType;

import java.time.Instant;
import java.util.Objects;

/** Immutable aggregate root recording a physical event emitted by an IoT node. */
public final class PhysicalEvent extends AbstractDomainAggregateRoot {

    private final Long id;
    private final Long companyId;
    private final Long sensorNodeId;
    private final Long zoneId;
    private final PhysicalEventType eventType;
    private final Instant occurredAt;
    private final boolean reconciled;

    private PhysicalEvent(Long id, Long companyId, Long sensorNodeId, Long zoneId,
                          PhysicalEventType eventType, Instant occurredAt, boolean reconciled) {
        this.id = id;
        this.companyId = requirePositive(companyId, "sensor.error.company.required");
        this.sensorNodeId = requirePositive(sensorNodeId, "sensor.error.sensorNode.required");
        this.zoneId = requirePositive(zoneId, "sensor.error.zone.required");
        this.eventType = Objects.requireNonNull(eventType, "sensor.error.eventType.invalid");
        this.occurredAt = Objects.requireNonNull(occurredAt, "sensor.error.occurredAt.required");
        this.reconciled = reconciled;
    }

    public static PhysicalEvent create(Long companyId, Long sensorNodeId, Long zoneId,
                                       PhysicalEventType eventType, Instant occurredAt) {
        PhysicalEvent event = new PhysicalEvent(null, companyId, sensorNodeId, zoneId, eventType, occurredAt, false);
        event.registerEvent(new PhysicalEventReceivedEvent(companyId, sensorNodeId, zoneId, eventType, occurredAt));
        return event;
    }

    public static PhysicalEvent restore(Long id, Long companyId, Long sensorNodeId, Long zoneId,
                                        PhysicalEventType eventType, Instant occurredAt, boolean reconciled) {
        if (id == null || id <= 0) throw new IllegalArgumentException("sensor.error.event.id.invalid");
        return new PhysicalEvent(id, companyId, sensorNodeId, zoneId, eventType, occurredAt, reconciled);
    }

    private static Long requirePositive(Long value, String key) {
        if (value == null || value <= 0) throw new IllegalArgumentException(key);
        return value;
    }

    public Long getId() { return id; }
    public Long getCompanyId() { return companyId; }
    public Long getSensorNodeId() { return sensorNodeId; }
    public Long getZoneId() { return zoneId; }
    public PhysicalEventType getEventType() { return eventType; }
    public Instant getOccurredAt() { return occurredAt; }
    public boolean isReconciled() { return reconciled; }
}
