package com.safezone.nodesecure.platform.inventory.application.commands;

import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.StockMovementType;

public record RegisterStockMovementCommand(Long productId, Long warehouseId, StockMovementType type,
                                           int quantity, String performedBy, String justification) {
    public RegisterStockMovementCommand {
        if (productId == null || productId <= 0)
            throw new IllegalArgumentException("stock.error.product.invalid");
        if (warehouseId == null || warehouseId <= 0)
            throw new IllegalArgumentException("stock.error.warehouse.invalid");
        if (type == null)
            throw new IllegalArgumentException("stock.error.type.required");
    }
}
