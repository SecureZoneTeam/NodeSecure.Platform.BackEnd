package com.safezone.nodesecure.platform.inventory.application.queries;

public record GetAllProductsQuery(Long companyId) {
    public GetAllProductsQuery {
        if (companyId != null && companyId <= 0) {
            throw new IllegalArgumentException("product.error.companyId.invalid");
        }
    }
}
