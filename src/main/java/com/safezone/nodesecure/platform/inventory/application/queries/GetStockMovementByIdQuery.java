package com.safezone.nodesecure.platform.inventory.application.queries;

public record GetStockMovementByIdQuery(Long id) {
    public GetStockMovementByIdQuery {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("stock.error.id.invalid");
        }
    }
}
