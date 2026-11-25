package com.safezone.nodesecure.platform.inventory.application.queries;

public record GetProductByIdQuery(Long id, Long companyId) {
    public GetProductByIdQuery {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("product.error.id.invalid");
        }
        if (companyId == null || companyId <= 0) {
            throw new IllegalArgumentException("product.error.companyId.invalid");
        }
    }
}
