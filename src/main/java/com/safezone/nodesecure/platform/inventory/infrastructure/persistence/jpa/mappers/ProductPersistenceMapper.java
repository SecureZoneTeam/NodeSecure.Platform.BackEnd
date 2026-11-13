package com.safezone.nodesecure.platform.inventory.infrastructure.persistence.jpa.mappers;

import com.safezone.nodesecure.platform.inventory.domain.model.aggregates.Product;
import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.ProductCategory;
import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.Sku;
import com.safezone.nodesecure.platform.inventory.infrastructure.persistence.jpa.entities.ProductJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductPersistenceMapper {
    public ProductJpaEntity toEntity(Product product) {
        return new ProductJpaEntity(product.getId(), product.getCompanyId(), product.getName(),
                product.getSku().value(), product.getCategory().name(), product.getUnit(),
                product.getCurrentStock(), product.getCreatedAt(), product.getUpdatedAt(), product.getCreatedBy());
    }

    public Product toDomain(ProductJpaEntity entity) {
        return Product.rehydrate(entity.getId(), entity.getCompanyId(), entity.getName(), new Sku(entity.getSku()),
                ProductCategory.valueOf(entity.getCategory()), entity.getUnit(), entity.getCurrentStock(),
                entity.getCreatedAt(), entity.getUpdatedAt(), entity.getCreatedBy());
    }
}
