package com.safezone.nodesecure.platform.inventory.application.queries;

public record GetProductByIdQuery(Long id) {
    public GetProductByIdQuery {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("product.error.id.invalid");
        }
    }
}
