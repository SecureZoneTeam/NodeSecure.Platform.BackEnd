package com.safezone.nodesecure.platform.inventory.application.queries;

import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.StockMovementType;

public record GetAllStockMovementsQuery(Long productId, Long warehouseId, StockMovementType type) {
    public GetAllStockMovementsQuery {
        if (productId != null && productId <= 0) {
            throw new IllegalArgumentException("stock.error.product.invalid");
        }
        if (warehouseId != null && warehouseId <= 0) {
            throw new IllegalArgumentException("stock.error.warehouse.invalid");
        }
    }
}
