package com.safezone.nodesecure.platform.inventory.interfaces.rest.transform;

import com.safezone.nodesecure.platform.inventory.application.commands.RegisterStockMovementCommand;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.resources.RegisterStockMovementResource;

public class RegisterStockMovementCommandMapper {
    public static RegisterStockMovementCommand toCommandFromResource(RegisterStockMovementResource resource) {
        return new RegisterStockMovementCommand(
                resource.productId(),
                resource.warehouseId(),
                InventoryValueObjectMapper.toStockMovementTypeFromString(resource.type()),
                resource.quantity(),
                resource.performedBy(),
                resource.justification());
    }
}
