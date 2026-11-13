package com.safezone.nodesecure.platform.sensors.application.commandservices;

public sealed interface SensorCommandFailure permits
        SensorCommandFailure.NotFound,
        SensorCommandFailure.UnregisteredMacAddress,
        SensorCommandFailure.Conflict,
        SensorCommandFailure.Validation {

    String messageKey();

    record NotFound(String resource, Long id) implements SensorCommandFailure {
        @Override public String messageKey() { return "sensor.error.notFound"; }
    }

    record UnregisteredMacAddress(String macAddress) implements SensorCommandFailure {
        @Override public String messageKey() { return "sensor.error.macAddress.notRegistered"; }
    }

    record Conflict(String messageKey) implements SensorCommandFailure {
    }

    record Validation(String messageKey) implements SensorCommandFailure {
    }
}
