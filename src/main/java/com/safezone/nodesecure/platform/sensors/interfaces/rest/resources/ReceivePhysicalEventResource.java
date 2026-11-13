package com.safezone.nodesecure.platform.sensors.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(name = "ReceivePhysicalEventRequest", description = "ESP32 event payload. The aliases support MAC/state/timestamp payloads.")
public record ReceivePhysicalEventResource(
        @NotBlank @JsonAlias("mac") @Schema(example = "A4:CF:12:9B:1E:44") String macAddress,
        @NotBlank @JsonAlias("state") @Schema(example = "DOOR_OPENED") String eventType,
        @NotNull @JsonAlias("timestamp") @Schema(example = "2026-11-11T22:30:00Z") Instant occurredAt) {
}
