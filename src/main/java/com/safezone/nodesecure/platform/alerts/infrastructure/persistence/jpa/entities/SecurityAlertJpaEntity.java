package com.safezone.nodesecure.platform.alerts.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "security_alert", indexes = {
        @Index(name = "idx_security_alert_status", columnList = "status"),
        @Index(name = "idx_security_alert_warehouse_id", columnList = "warehouse_id"),
        @Index(name = "idx_security_alert_raised_at", columnList = "raised_at")
})
public class SecurityAlertJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "warehouse_id", nullable = false, updatable = false)
    private Long warehouseId;
    @Column(name = "zone_id", updatable = false)
    private Long zoneId;
    @Column(name = "physical_event_id", updatable = false)
    private Long physicalEventId;
    @Column(name = "alert_type", nullable = false, updatable = false, length = 30)
    private String alertType;
    @Column(name = "severity", nullable = false, updatable = false, length = 20)
    private String severity;
    @Column(name = "status", nullable = false, length = 20)
    private String status;
    @Column(name = "message", nullable = false, updatable = false, length = 255)
    private String message;
    @Column(name = "raised_at", nullable = false, updatable = false)
    private Instant raisedAt;
    @Column(name = "justification", length = 300)
    private String justification;
    @Column(name = "resolved_at")
    private Instant resolvedAt;
    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    protected SecurityAlertJpaEntity() {
    }

    public SecurityAlertJpaEntity(Long id, Long warehouseId, Long zoneId, Long physicalEventId, String alertType,
                                  String severity, String status, String message, Instant raisedAt,
                                  String justification, Instant resolvedAt, String resolvedBy) {
        this.id = id;
        this.warehouseId = warehouseId;
        this.zoneId = zoneId;
        this.physicalEventId = physicalEventId;
        this.alertType = alertType;
        this.severity = severity;
        this.status = status;
        this.message = message;
        this.raisedAt = raisedAt;
        this.justification = justification;
        this.resolvedAt = resolvedAt;
        this.resolvedBy = resolvedBy;
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

    public String getAlertType() {
        return alertType;
    }

    public String getSeverity() {
        return severity;
    }

    public String getStatus() {
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
