package com.safezone.nodesecure.platform.inventory.application.commands;

import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.ProductCategory;
import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.Sku;

public record CreateProductCommand(Long companyId, String name, Sku sku, ProductCategory category,
                                   String unit, String createdBy) {
    public CreateProductCommand {
        if (companyId == null || companyId <= 0)
            throw new IllegalArgumentException("product.error.companyId.invalid");
        if (sku == null)
            throw new IllegalArgumentException("product.error.sku.invalid");
        if (category == null)
            throw new IllegalArgumentException("product.error.category.required");
    }
}
