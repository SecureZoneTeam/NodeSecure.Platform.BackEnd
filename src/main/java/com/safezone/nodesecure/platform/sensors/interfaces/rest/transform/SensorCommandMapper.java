package com.safezone.nodesecure.platform.sensors.interfaces.rest.transform;

import com.safezone.nodesecure.platform.sensors.application.commands.RegisterSensorNodeCommand;
import com.safezone.nodesecure.platform.sensors.application.commands.UpdateSensorNodeCommand;
import com.safezone.nodesecure.platform.sensors.application.commands.ReceivePhysicalEventCommand;
import com.safezone.nodesecure.platform.sensors.interfaces.rest.resources.*;

public final class SensorCommandMapper {
    private SensorCommandMapper() { }

    public static RegisterSensorNodeCommand toRegisterCommand(CreateSensorNodeResource resource, Long companyId) {
        return new RegisterSensorNodeCommand(companyId, resource.zoneId(), resource.macAddress(), resource.model(), resource.sensorType());
    }

    public static UpdateSensorNodeCommand toUpdateCommand(Long id, UpdateSensorNodeResource resource, Long companyId) {
        return new UpdateSensorNodeCommand(id, companyId, resource.zoneId(), resource.macAddress(),
                resource.model(), resource.sensorType());
    }

    public static ReceivePhysicalEventCommand toReceiveEventCommand(ReceivePhysicalEventResource resource) {
        return new ReceivePhysicalEventCommand(resource.macAddress(), resource.eventType(), resource.occurredAt());
    }
}
