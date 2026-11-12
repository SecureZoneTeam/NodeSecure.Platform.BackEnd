package com.safezone.nodesecure.platform.sensors.interfaces.rest.resources;

import com.safezone.nodesecure.platform.sensors.domain.model.valueobjects.SensorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateSensorNodeResource(
        @NotNull @Positive Long zoneId,
        @NotBlank @Size(max = 17) String macAddress,
        @NotBlank @Size(max = 100) String model,
        @NotNull SensorType sensorType) {
}
