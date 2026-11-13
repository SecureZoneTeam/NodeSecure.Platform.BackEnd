package com.safezone.nodesecure.platform.inventory.interfaces.rest.transform;

import com.safezone.nodesecure.platform.inventory.domain.model.aggregates.Product;
import com.safezone.nodesecure.platform.inventory.interfaces.rest.resources.ProductResource;

public class ProductResourceMapper {
    public static ProductResource toResourceFromEntity(Product entity) {
        return new ProductResource(
                entity.getId(),
                entity.getCompanyId(),
                entity.getName(),
                entity.getSku().value(),
                entity.getCategory().name(),
                entity.getUnit(),
                entity.getCurrentStock(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy());
    }
}
