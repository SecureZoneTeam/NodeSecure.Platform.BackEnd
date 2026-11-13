package com.safezone.nodesecure.platform.sensors.application.commands;

import java.time.Instant;

public record ReceivePhysicalEventCommand(String macAddress, String eventType, Instant occurredAt) {
}
