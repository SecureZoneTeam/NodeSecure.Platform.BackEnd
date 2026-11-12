package com.safezone.nodesecure.platform.warehouses.interfaces.rest.transform;

import com.safezone.nodesecure.platform.warehouses.application.commands.*;
import com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects.OperatingShift;
import com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects.StreetAddress;
import com.safezone.nodesecure.platform.warehouses.interfaces.rest.resources.*;

public final class WarehouseCommandMapper {
    private WarehouseCommandMapper() { }

    public static CreateWarehouseCommand toCreateCommand(CreateWarehouseResource resource, Long companyId) {
        return new CreateWarehouseCommand(companyId,
                resource.name(), new StreetAddress(resource.street(), resource.district(), resource.city(), resource.country()));
    }

    public static UpdateWarehouseCommand toUpdateCommand(Long id, UpdateWarehouseResource resource, Long companyId) {
        return new UpdateWarehouseCommand(id, companyId, resource.name(),
                new StreetAddress(resource.street(), resource.district(), resource.city(), resource.country()), resource.status());
    }

    public static CreateWarehouseZoneCommand toCreateZoneCommand(CreateWarehouseZoneResource resource, Long companyId) {
        return new CreateWarehouseZoneCommand(companyId, resource.warehouseId(), resource.name(), resource.code(),
                resource.riskLevel(), OperatingShift.from(resource.openingShiftStart(), resource.openingShiftEnd()));
    }

    public static UpdateWarehouseZoneCommand toUpdateZoneCommand(Long id, UpdateWarehouseZoneResource resource, Long companyId) {
        return new UpdateWarehouseZoneCommand(companyId, id, resource.name(), resource.code(), resource.riskLevel(),
                OperatingShift.from(resource.openingShiftStart(), resource.openingShiftEnd()));
    }
}
