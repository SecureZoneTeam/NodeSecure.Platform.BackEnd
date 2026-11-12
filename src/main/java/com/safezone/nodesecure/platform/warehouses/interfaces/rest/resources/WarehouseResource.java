package com.safezone.nodesecure.platform.warehouses.interfaces.rest.resources;

import com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects.WarehouseStatus;

public record WarehouseResource(Long id, Long companyId, String name, String street, String district,
                                String city, String country, WarehouseStatus status, int zoneCount) {
}
