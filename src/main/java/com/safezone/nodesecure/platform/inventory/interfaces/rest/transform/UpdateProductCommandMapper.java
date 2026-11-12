package com.safezone.nodesecure.platform.inventory.interfaces.rest.transform;

import com.safezone.nodesecure.platform.inventory.application.commands.UpdateProductCommand;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.resources.UpdateProductResource;

public class UpdateProductCommandMapper {
    public static UpdateProductCommand toCommandFromResource(Long id, UpdateProductResource resource) {
        return new UpdateProductCommand(
                id,
                resource.name(),
                InventoryValueObjectMapper.toSkuFromString(resource.sku()),
                InventoryValueObjectMapper.toProductCategoryFromString(resource.category()),
                resource.unit());
    }
}
