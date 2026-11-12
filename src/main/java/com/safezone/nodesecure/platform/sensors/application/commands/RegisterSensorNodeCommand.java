package com.safezone.nodesecure.platform.sensors.application.commands;

import com.safezone.nodesecure.platform.sensors.domain.model.valueobjects.SensorType;

public record RegisterSensorNodeCommand(Long companyId, Long zoneId, String macAddress, String model, SensorType sensorType) {
}
