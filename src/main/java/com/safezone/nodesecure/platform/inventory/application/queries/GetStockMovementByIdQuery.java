package com.safezone.nodesecure.platform.inventory.application.queries;

import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.StockMovementType;

import java.util.Set;

public record GetStockMovementByIdQuery(Long id, Long companyId, Set<Long> warehouseScope,
                                        Set<StockMovementType> allowedTypes) {
    public GetStockMovementByIdQuery {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("stock.error.id.invalid");
        }
        if (companyId == null || companyId <= 0) {
            throw new IllegalArgumentException("product.error.companyId.invalid");
        }
        allowedTypes = allowedTypes == null ? Set.of() : Set.copyOf(allowedTypes);
    }
}
