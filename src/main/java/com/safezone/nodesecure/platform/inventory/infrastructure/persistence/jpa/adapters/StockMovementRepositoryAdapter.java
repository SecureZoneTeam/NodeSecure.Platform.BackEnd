package com.safezone.nodesecure.platform.inventory.infrastructure.persistence.jpa.adapters;

import com.safezone.nodesecure.platform.inventory.domain.model.aggregates.StockMovement;
import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.StockMovementType;
import com.safezone.nodesecure.platform.inventory.domain.repositories.StockMovementRepository;
import com.safezone.nodesecure.platform.inventory.infrastructure.persistence.jpa.StockMovementJpaRepository;
import com.safezone.nodesecure.platform.inventory.infrastructure.persistence.jpa.entities.StockMovementJpaEntity;
import com.safezone.nodesecure.platform.inventory.infrastructure.persistence.jpa.mappers.StockMovementPersistenceMapper;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class StockMovementRepositoryAdapter implements StockMovementRepository {
    private final StockMovementJpaRepository jpa;
    private final StockMovementPersistenceMapper mapper;

    public StockMovementRepositoryAdapter(StockMovementJpaRepository jpa, StockMovementPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public StockMovement save(StockMovement movement) {
        return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(movement)));
    }

    @Override
    public Optional<StockMovement> findById(Long id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<StockMovement> findAll(Long productId, Long warehouseId, StockMovementType type) {
        Specification<StockMovementJpaEntity> spec = (root, query, cb) -> cb.conjunction();
        if (productId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("productId"), productId));
        }
        if (warehouseId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("warehouseId"), warehouseId));
        }
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type.name()));
        }
        var newestFirst = Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"));
        return jpa.findAll(spec, newestFirst).stream().map(mapper::toDomain).toList();
    }
}
