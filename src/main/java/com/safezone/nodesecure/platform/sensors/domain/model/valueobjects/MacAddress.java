package com.safezone.nodesecure.platform.sensors.domain.model.valueobjects;

import java.util.Locale;
import java.util.regex.Pattern;

/** Normalized MAC address used as the unique hardware identity of an ESP32 node. */
public record MacAddress(String value) {
    private static final Pattern FORMAT = Pattern.compile("^([0-9A-F]{2}:){5}[0-9A-F]{2}$");

    public MacAddress {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("sensor.error.macAddress.required");
        value = value.trim().toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) throw new IllegalArgumentException("sensor.error.macAddress.invalid");
    }

    @Override
    public String toString() { return value; }
}
