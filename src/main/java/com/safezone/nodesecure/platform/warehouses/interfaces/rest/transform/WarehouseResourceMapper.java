package com.safezone.nodesecure.platform.warehouses.interfaces.rest.transform;

import com.safezone.nodesecure.platform.warehouses.domain.model.aggregates.Warehouse;
import com.safezone.nodesecure.platform.warehouses.domain.model.entities.WarehouseZone;
import com.safezone.nodesecure.platform.warehouses.interfaces.rest.resources.WarehouseResource;
import com.safezone.nodesecure.platform.warehouses.interfaces.rest.resources.WarehouseZoneResource;

public final class WarehouseResourceMapper {
    private WarehouseResourceMapper() { }

    public static WarehouseResource toResource(Warehouse warehouse) {
        return new WarehouseResource(warehouse.getId(), warehouse.getCompanyId(), warehouse.getName(),
                warehouse.getAddress().street(), warehouse.getAddress().district(), warehouse.getAddress().city(),
                warehouse.getAddress().country(), warehouse.getStatus(), warehouse.getZoneCount());
    }

    public static WarehouseZoneResource toZoneResource(WarehouseZone zone) {
        return new WarehouseZoneResource(zone.getId(), zone.getWarehouseId(), zone.getName(), zone.getCode(),
                zone.getRiskLevel(), zone.getOperatingShift().startText(), zone.getOperatingShift().endText());
    }
}
