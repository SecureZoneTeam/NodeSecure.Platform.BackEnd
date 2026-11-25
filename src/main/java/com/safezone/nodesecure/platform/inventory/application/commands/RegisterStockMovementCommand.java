package com.safezone.nodesecure.platform.inventory.application.commands;

import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.StockMovementType;

/**
 * {@code companyId} and {@code performedBy} come from the authenticated session.
 * The product and the warehouse must belong to that organization.
 */
public record RegisterStockMovementCommand(Long companyId, Long productId, Long warehouseId, StockMovementType type,
                                           int quantity, String performedBy, String justification) {
    public RegisterStockMovementCommand {
        if (companyId == null || companyId <= 0)
            throw new IllegalArgumentException("product.error.companyId.invalid");
        if (productId == null || productId <= 0)
            throw new IllegalArgumentException("stock.error.product.invalid");
        if (warehouseId == null || warehouseId <= 0)
            throw new IllegalArgumentException("stock.error.warehouse.invalid");
        if (type == null)
            throw new IllegalArgumentException("stock.error.type.required");
    }
}
