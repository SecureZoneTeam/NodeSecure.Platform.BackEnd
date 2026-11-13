package com.safezone.nodesecure.platform.inventory.interfaces.rest.transform;

import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.ProductCategory;
import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.Sku;
import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.StockMovementType;

public class InventoryValueObjectMapper {
    public static Sku toSkuFromString(String value) {
        return new Sku(value);
    }

    public static ProductCategory toProductCategoryFromString(String value) {
        return ProductCategory.fromValue(value);
    }

    public static StockMovementType toStockMovementTypeFromString(String value) {
        return StockMovementType.fromValue(value);
    }
}
