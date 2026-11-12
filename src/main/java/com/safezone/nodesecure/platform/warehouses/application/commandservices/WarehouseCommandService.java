package com.safezone.nodesecure.platform.warehouses.application.commandservices;

import com.safezone.nodesecure.platform.shared.application.result.Result;
import com.safezone.nodesecure.platform.warehouses.application.commands.*;
import com.safezone.nodesecure.platform.warehouses.domain.model.aggregates.Warehouse;
import com.safezone.nodesecure.platform.warehouses.domain.model.entities.WarehouseZone;

public interface WarehouseCommandService {
    Result<Warehouse, WarehouseCommandFailure> handle(CreateWarehouseCommand command);
    Result<Warehouse, WarehouseCommandFailure> handle(UpdateWarehouseCommand command);
    Result<Long, WarehouseCommandFailure> handle(DeleteWarehouseCommand command);
    Result<WarehouseZone, WarehouseCommandFailure> handle(CreateWarehouseZoneCommand command);
    Result<WarehouseZone, WarehouseCommandFailure> handle(UpdateWarehouseZoneCommand command);
    Result<Long, WarehouseCommandFailure> handle(DeleteWarehouseZoneCommand command);
}
