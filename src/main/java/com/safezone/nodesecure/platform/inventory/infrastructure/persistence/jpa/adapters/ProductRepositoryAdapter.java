package com.safezone.nodesecure.platform.inventory.infrastructure.persistence.jpa.adapters;

import com.safezone.nodesecure.platform.inventory.domain.exceptions.DuplicateSkuException;
import com.safezone.nodesecure.platform.inventory.domain.model.aggregates.Product;
import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.Sku;
import com.safezone.nodesecure.platform.inventory.domain.repositories.ProductRepository;
import com.safezone.nodesecure.platform.inventory.infrastructure.persistence.jpa.ProductJpaRepository;
import com.safezone.nodesecure.platform.inventory.infrastructure.persistence.jpa.mappers.ProductPersistenceMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ProductRepositoryAdapter implements ProductRepository {
    private final ProductJpaRepository jpa;
    private final ProductPersistenceMapper mapper;

    public ProductRepositoryAdapter(ProductJpaRepository jpa, ProductPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Product save(Product product) {
        try {
            return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(product)));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateSkuException(e);
        }
    }

    @Override
    public Optional<Product> findById(Long id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Product> findByIdForUpdate(Long id) {
        return jpa.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public List<Product> findAll() {
        return jpa.findAllByOrderByIdAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Product> findAllByCompanyId(Long companyId) {
        return jpa.findAllByCompanyIdOrderByIdAsc(companyId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByCompanyIdAndSku(Long companyId, Sku sku) {
        return jpa.existsByCompanyIdAndSku(companyId, sku.value());
    }

    @Override
    public boolean existsByCompanyIdAndSkuAndIdNot(Long companyId, Sku sku, Long id) {
        return jpa.existsByCompanyIdAndSkuAndIdNot(companyId, sku.value(), id);
    }
}
