package com.safezone.nodesecure.platform.sensors.application.commandservices;

import com.safezone.nodesecure.platform.shared.application.result.Result;
import com.safezone.nodesecure.platform.sensors.application.commands.*;
import com.safezone.nodesecure.platform.sensors.domain.model.aggregates.PhysicalEvent;
import com.safezone.nodesecure.platform.sensors.domain.model.aggregates.SensorNode;

public interface SensorCommandService {
    Result<SensorNode, SensorCommandFailure> handle(RegisterSensorNodeCommand command);
    Result<SensorNode, SensorCommandFailure> handle(UpdateSensorNodeCommand command);
    Result<Long, SensorCommandFailure> handle(DeleteSensorNodeCommand command);
    Result<PhysicalEvent, SensorCommandFailure> handle(ReceivePhysicalEventCommand command);
}
