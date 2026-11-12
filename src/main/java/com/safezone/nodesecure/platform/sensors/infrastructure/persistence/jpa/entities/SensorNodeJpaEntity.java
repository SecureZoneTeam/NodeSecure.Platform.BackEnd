package com.safezone.nodesecure.platform.sensors.infrastructure.persistence.jpa.entities;

import com.safezone.nodesecure.platform.sensors.domain.model.valueobjects.ConnectionStatus;
import com.safezone.nodesecure.platform.sensors.domain.model.valueobjects.SensorType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "sensor_nodes", uniqueConstraints = @UniqueConstraint(name = "uk_sensor_node_mac_address", columnNames = "mac_address"))
public class SensorNodeJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "company_id", nullable = false)
    private Long companyId;
    @Column(name = "zone_id", nullable = false)
    private Long zoneId;
    @Column(name = "mac_address", nullable = false, length = 17)
    private String macAddress;
    @Column(nullable = false, length = 100)
    private String model;
    @Enumerated(EnumType.STRING) @Column(name = "sensor_type", nullable = false, length = 30)
    private SensorType sensorType;
    @Enumerated(EnumType.STRING) @Column(name = "connection_status", nullable = false, length = 20)
    private ConnectionStatus connectionStatus;
    @Column(name = "last_ping_at")
    private Instant lastPingAt;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    public SensorNodeJpaEntity() { }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getZoneId() { return zoneId; }
    public void setZoneId(Long zoneId) { this.zoneId = zoneId; }
    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public SensorType getSensorType() { return sensorType; }
    public void setSensorType(SensorType sensorType) { this.sensorType = sensorType; }
    public ConnectionStatus getConnectionStatus() { return connectionStatus; }
    public void setConnectionStatus(ConnectionStatus connectionStatus) { this.connectionStatus = connectionStatus; }
    public Instant getLastPingAt() { return lastPingAt; }
    public void setLastPingAt(Instant lastPingAt) { this.lastPingAt = lastPingAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
