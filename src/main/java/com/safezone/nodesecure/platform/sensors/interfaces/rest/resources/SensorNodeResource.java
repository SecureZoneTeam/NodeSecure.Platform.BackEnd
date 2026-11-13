package com.safezone.nodesecure.platform.sensors.interfaces.rest.resources;

import com.safezone.nodesecure.platform.sensors.domain.model.valueobjects.ConnectionStatus;
import com.safezone.nodesecure.platform.sensors.domain.model.valueobjects.SensorType;

import java.time.Instant;

public record SensorNodeResource(Long id, Long zoneId, String macAddress, String model,
                                 SensorType sensorType, ConnectionStatus connectionStatus, Instant lastPingAt) {
}
