package com.safezone.nodesecure.platform.sensors.domain.model.valueobjects;

public enum SensorType {
    MAGNETIC_CONTACT,
    MOTION,
    VIBRATION,
    RFIDReader;

    public static SensorType from(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("sensor.error.type.invalid");
        try { return valueOf(value.trim()); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("sensor.error.type.invalid"); }
    }
}
