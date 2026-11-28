package com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects;

/** Resource limited by a plan. {@link #key()} is the name used in API responses. */
public enum PlanResource {
    WAREHOUSES("warehouses"),
    SENSOR_NODES("sensorNodes"),
    USERS("users");

    private final String key;

    PlanResource(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
