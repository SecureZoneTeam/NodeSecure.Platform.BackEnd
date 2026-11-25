package com.safezone.nodesecure.platform.inventory.application.commands;

import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.ProductCategory;
import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.Sku;

/** A product of another organization behaves as missing. */
public record UpdateProductCommand(Long companyId, Long id, String name, Sku sku, ProductCategory category,
                                   String unit) {
    public UpdateProductCommand {
        if (companyId == null || companyId <= 0)
            throw new IllegalArgumentException("product.error.companyId.invalid");
        if (id == null || id <= 0)
            throw new IllegalArgumentException("product.error.id.invalid");
        if (sku == null)
            throw new IllegalArgumentException("product.error.sku.invalid");
        if (category == null)
            throw new IllegalArgumentException("product.error.category.required");
    }
}
