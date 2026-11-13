package com.safezone.nodesecure.platform.inventory.domain.repositories;

import com.safezone.nodesecure.platform.inventory.domain.model.aggregates.Product;
import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.Sku;

import java.util.List;
import java.util.Optional;


public interface ProductRepository {
    Product save(Product product);

    Optional<Product> findById(Long id);

    Optional<Product> findByIdForUpdate(Long id);

    List<Product> findAll();

    List<Product> findAllByCompanyId(Long companyId);

    boolean existsByCompanyIdAndSku(Long companyId, Sku sku);

    boolean existsByCompanyIdAndSkuAndIdNot(Long companyId, Sku sku, Long id);
}
