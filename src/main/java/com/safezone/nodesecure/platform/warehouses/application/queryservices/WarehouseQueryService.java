package com.safezone.nodesecure.platform.warehouses.application.queryservices;

import com.safezone.nodesecure.platform.warehouses.application.queries.*;
import com.safezone.nodesecure.platform.warehouses.domain.model.aggregates.Warehouse;
import com.safezone.nodesecure.platform.warehouses.domain.model.entities.WarehouseZone;

import java.util.List;
import java.util.Optional;

public interface WarehouseQueryService {
    List<Warehouse> handle(GetAllWarehousesQuery query);
    Optional<Warehouse> handle(GetWarehouseByIdQuery query);
    List<WarehouseZone> handle(GetAllWarehouseZonesQuery query);
    Optional<WarehouseZone> handle(GetWarehouseZoneByIdQuery query);
}
