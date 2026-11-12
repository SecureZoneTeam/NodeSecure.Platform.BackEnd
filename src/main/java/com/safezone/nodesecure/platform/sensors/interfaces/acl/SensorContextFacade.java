package com.safezone.nodesecure.platform.sensors.interfaces.acl;

/** Public contract exposed to other bounded contexts without leaking sensor persistence details. */
public interface SensorContextFacade {
    boolean zoneHasRegisteredSensors(Long zoneId);
}
