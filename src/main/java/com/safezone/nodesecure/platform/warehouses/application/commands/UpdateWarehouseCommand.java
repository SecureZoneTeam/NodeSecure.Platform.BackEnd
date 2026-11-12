package com.safezone.nodesecure.platform.warehouses.application.commands;

import com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects.StreetAddress;
import com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects.WarehouseStatus;

public record UpdateWarehouseCommand(Long id, Long companyId, String name, StreetAddress address,
                                     WarehouseStatus status) {
}
