package com.safezone.nodesecure.platform.inventory.interfaces.rest.transform;

import com.safezone.nodesecure.platform.inventory.application.commands.CreateProductCommand;
import com.safezone.nodesecure.platform.inventory.application.commands.RegisterStockMovementCommand;
import com.safezone.nodesecure.platform.inventory.application.commands.UpdateProductCommand;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.resources.CreateProductResource;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.resources.RegisterStockMovementResource;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.resources.UpdateProductResource;

/**
 * Builds inventory commands. The organization and the acting username are always supplied by the
 * controller from the authenticated session; the equivalent request fields are ignored.
 */
public final class InventoryCommandMapper {

    private InventoryCommandMapper() {
    }

    public static CreateProductCommand toCreateProductCommand(CreateProductResource resource, Long companyId,
                                                              String username) {
        return new CreateProductCommand(
                companyId,
                resource.name(),
                InventoryValueObjectMapper.toSkuFromString(resource.sku()),
                InventoryValueObjectMapper.toProductCategoryFromString(resource.category()),
                resource.unit(),
                username);
    }

    public static UpdateProductCommand toUpdateProductCommand(Long id, UpdateProductResource resource, Long companyId) {
        return new UpdateProductCommand(
                companyId,
                id,
                resource.name(),
                InventoryValueObjectMapper.toSkuFromString(resource.sku()),
                InventoryValueObjectMapper.toProductCategoryFromString(resource.category()),
                resource.unit());
    }

    public static RegisterStockMovementCommand toRegisterStockMovementCommand(RegisterStockMovementResource resource,
                                                                              Long companyId, String username) {
        return new RegisterStockMovementCommand(
                companyId,
                resource.productId(),
                resource.warehouseId(),
                InventoryValueObjectMapper.toStockMovementTypeFromString(resource.type()),
                resource.quantity(),
                username,
                resource.justification());
    }
}
