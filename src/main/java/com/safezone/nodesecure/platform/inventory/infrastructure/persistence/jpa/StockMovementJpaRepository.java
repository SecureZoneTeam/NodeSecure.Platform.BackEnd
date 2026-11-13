package com.safezone.nodesecure.platform.inventory.infrastructure.persistence.jpa;

import com.safezone.nodesecure.platform.inventory.infrastructure.persistence.jpa.entities.StockMovementJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StockMovementJpaRepository
        extends JpaRepository<StockMovementJpaEntity, Long>, JpaSpecificationExecutor<StockMovementJpaEntity> {
}
