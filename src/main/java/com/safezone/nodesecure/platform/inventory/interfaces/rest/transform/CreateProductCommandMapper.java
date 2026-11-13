package com.safezone.nodesecure.platform.inventory.interfaces.rest.transform;

import com.safezone.nodesecure.platform.inventory.application.commands.CreateProductCommand;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.resources.CreateProductResource;


public class CreateProductCommandMapper {
    public static CreateProductCommand toCommandFromResource(CreateProductResource resource) {
        return new CreateProductCommand(
                resource.companyId(),
                resource.name(),
                InventoryValueObjectMapper.toSkuFromString(resource.sku()),
                InventoryValueObjectMapper.toProductCategoryFromString(resource.category()),
                resource.unit(),
                resource.createdBy());
    }
}
