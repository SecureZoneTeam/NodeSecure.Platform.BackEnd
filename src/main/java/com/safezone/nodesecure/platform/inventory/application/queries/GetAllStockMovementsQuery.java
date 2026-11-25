package com.safezone.nodesecure.platform.inventory.application.queries;

import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.StockMovementType;

import java.util.Set;

/**
 * Movement history of one organization. {@code warehouseScope}: null = every warehouse (owner),
 * otherwise only the assigned warehouses. {@code allowedTypes} hides the types the user cannot view.
 */
public record GetAllStockMovementsQuery(Long companyId, Long productId, Long warehouseId, StockMovementType type,
                                        Set<Long> warehouseScope, Set<StockMovementType> allowedTypes) {
    public GetAllStockMovementsQuery {
        if (companyId == null || companyId <= 0) {
            throw new IllegalArgumentException("product.error.companyId.invalid");
        }
        if (productId != null && productId <= 0) {
            throw new IllegalArgumentException("stock.error.product.invalid");
        }
        if (warehouseId != null && warehouseId <= 0) {
            throw new IllegalArgumentException("stock.error.warehouse.invalid");
        }
        allowedTypes = allowedTypes == null ? Set.of() : Set.copyOf(allowedTypes);
    }
}
