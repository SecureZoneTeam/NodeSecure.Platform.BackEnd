package com.safezone.nodesecure.platform.sensors.domain.model.aggregates;

import com.safezone.nodesecure.platform.shared.domain.aggregates.AbstractDomainAggregateRoot;
import com.safezone.nodesecure.platform.sensors.domain.model.events.SensorNodeRegisteredEvent;
import com.safezone.nodesecure.platform.sensors.domain.model.valueobjects.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Aggregate root representing one registered IoT device. */
public final class SensorNode extends AbstractDomainAggregateRoot {

    private static final int MAX_MODEL_LENGTH = 100;

    private final Long id;
    private final Long companyId;
    private Long zoneId;
    private MacAddress macAddress;
    private String model;
    private SensorType sensorType;
    private ConnectionStatus connectionStatus;
    private Instant lastPingAt;
    private final Instant createdAt;
    private Instant updatedAt;

    private SensorNode(Long id, Long companyId, Long zoneId, MacAddress macAddress, String model,
                       SensorType sensorType, ConnectionStatus connectionStatus, Instant lastPingAt,
                       Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.companyId = requirePositive(companyId, "sensor.error.company.required");
        this.zoneId = requirePositive(zoneId, "sensor.error.zone.required");
        this.macAddress = Objects.requireNonNull(macAddress);
        this.model = validateModel(model);
        this.sensorType = Objects.requireNonNull(sensorType, "sensor.error.type.invalid");
        this.connectionStatus = Objects.requireNonNull(connectionStatus);
        this.lastPingAt = lastPingAt;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static SensorNode create(Long companyId, Long zoneId, MacAddress macAddress, String model,
                                    SensorType sensorType, Instant now) {
        SensorNode node = new SensorNode(null, companyId, zoneId, macAddress, model, sensorType,
                ConnectionStatus.OFFLINE, null, now, now);
        node.registerEvent(new SensorNodeRegisteredEvent(companyId, zoneId, macAddress.value(), sensorType, now));
        return node;
    }

    public static SensorNode restore(Long id, Long companyId, Long zoneId, MacAddress macAddress,
                                     String model, SensorType sensorType, ConnectionStatus connectionStatus,
                                     Instant lastPingAt, Instant createdAt, Instant updatedAt) {
        if (id == null || id <= 0) throw new IllegalArgumentException("sensor.error.id.invalid");
        return new SensorNode(id, companyId, zoneId, macAddress, model, sensorType, connectionStatus,
                lastPingAt, createdAt, updatedAt);
    }

    public void updateConfiguration(Long zoneId, MacAddress macAddress, String model,
                                    SensorType sensorType, Instant now) {
        this.zoneId = requirePositive(zoneId, "sensor.error.zone.required");
        this.macAddress = Objects.requireNonNull(macAddress, "sensor.error.macAddress.invalid");
        this.model = validateModel(model);
        this.sensorType = Objects.requireNonNull(sensorType, "sensor.error.type.invalid");
        this.updatedAt = Objects.requireNonNull(now);
    }

    public void registerPing(Instant receivedAt) {
        this.connectionStatus = ConnectionStatus.ONLINE;
        this.lastPingAt = Objects.requireNonNull(receivedAt);
        this.updatedAt = receivedAt;
    }

    public void markOffline(Instant now) {
        if (connectionStatus != ConnectionStatus.OFFLINE) {
            connectionStatus = ConnectionStatus.OFFLINE;
            updatedAt = Objects.requireNonNull(now);
        }
    }

    public boolean connectionExpired(Instant now, Duration timeout) {
        return lastPingAt == null || lastPingAt.plus(timeout).isBefore(now);
    }

    private static Long requirePositive(Long value, String key) {
        if (value == null || value <= 0) throw new IllegalArgumentException(key);
        return value;
    }

    private static String validateModel(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("sensor.error.model.required");
        String model = value.trim();
        if (model.length() > MAX_MODEL_LENGTH) throw new IllegalArgumentException("sensor.error.model.tooLong");
        return model;
    }

    public Long getId() { return id; }
    public Long getCompanyId() { return companyId; }
    public Long getZoneId() { return zoneId; }
    public MacAddress getMacAddress() { return macAddress; }
    public String getModel() { return model; }
    public SensorType getSensorType() { return sensorType; }
    public ConnectionStatus getConnectionStatus() { return connectionStatus; }
    public Instant getLastPingAt() { return lastPingAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
