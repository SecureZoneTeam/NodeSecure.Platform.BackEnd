package com.safezone.nodesecure.platform.warehouses.application.commands;

import com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects.StreetAddress;

public record CreateWarehouseCommand(Long companyId, String name, StreetAddress address) {
}
