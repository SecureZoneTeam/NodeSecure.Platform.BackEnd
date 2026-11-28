package com.safezone.nodesecure.platform.sensors.interfaces.acl;

/** Public contract exposed to other bounded contexts without leaking sensor persistence details. */
public interface SensorContextFacade {
    boolean zoneHasRegisteredSensors(Long zoneId);

    /** Sensor nodes registered by the organization (used to check plan limits). */
    long countSensorNodes(Long companyId);
}
