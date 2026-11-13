package com.safezone.nodesecure.platform.sensors.domain.model.valueobjects;

import java.util.Locale;

public enum PhysicalEventType {
    DOOR_OPENED,
    DOOR_CLOSED,
    NODE_DISCONNECTED;

    public static PhysicalEventType from(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("sensor.error.eventType.invalid");
        try { return valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("sensor.error.eventType.invalid"); }
    }
}
