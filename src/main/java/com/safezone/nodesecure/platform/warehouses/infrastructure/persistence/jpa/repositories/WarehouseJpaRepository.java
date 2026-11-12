package com.safezone.nodesecure.platform.warehouses.infrastructure.persistence.jpa.repositories;

import com.safezone.nodesecure.platform.warehouses.infrastructure.persistence.jpa.entities.WarehouseJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseJpaRepository extends JpaRepository<WarehouseJpaEntity, Long> {
    List<WarehouseJpaEntity> findAllByCompanyIdOrderByIdAsc(Long companyId);
    Optional<WarehouseJpaEntity> findByIdAndCompanyId(Long id, Long companyId);
    Optional<WarehouseJpaEntity> findByCompanyIdAndZones_Id(Long companyId, Long zoneId);
    Optional<WarehouseJpaEntity> findByZones_Id(Long zoneId);
    boolean existsByCompanyIdAndNameIgnoreCase(Long companyId, String name);
    boolean existsByCompanyIdAndNameIgnoreCaseAndIdIsNot(Long companyId, String name, Long id);
}
