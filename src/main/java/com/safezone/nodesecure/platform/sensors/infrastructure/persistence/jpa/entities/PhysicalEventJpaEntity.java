package com.safezone.nodesecure.platform.sensors.infrastructure.persistence.jpa.entities;

import com.safezone.nodesecure.platform.sensors.domain.model.valueobjects.PhysicalEventType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "physical_events", indexes = @Index(name = "idx_physical_event_company_time", columnList = "company_id, occurred_at"))
public class PhysicalEventJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "company_id", nullable = false)
    private Long companyId;
    @Column(name = "sensor_node_id", nullable = false)
    private Long sensorNodeId;
    @Column(name = "zone_id", nullable = false)
    private Long zoneId;
    @Enumerated(EnumType.STRING) @Column(name = "event_type", nullable = false, length = 30)
    private PhysicalEventType eventType;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
    @Column(nullable = false)
    private boolean reconciled;

    public PhysicalEventJpaEntity() { }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getSensorNodeId() { return sensorNodeId; }
    public void setSensorNodeId(Long sensorNodeId) { this.sensorNodeId = sensorNodeId; }
    public Long getZoneId() { return zoneId; }
    public void setZoneId(Long zoneId) { this.zoneId = zoneId; }
    public PhysicalEventType getEventType() { return eventType; }
    public void setEventType(PhysicalEventType eventType) { this.eventType = eventType; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public boolean isReconciled() { return reconciled; }
    public void setReconciled(boolean reconciled) { this.reconciled = reconciled; }
}
