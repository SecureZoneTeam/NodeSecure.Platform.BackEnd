package com.safezone.nodesecure.platform.warehouses.domain.repositories;

import com.safezone.nodesecure.platform.warehouses.domain.model.aggregates.Warehouse;

import java.util.List;
import java.util.Optional;

/** Domain repository port for Warehouse aggregates. */
public interface WarehouseRepository {
    Warehouse save(Warehouse warehouse);
    Optional<Warehouse> findByIdAndCompanyId(Long warehouseId, Long companyId);
    Optional<Warehouse> findByZoneIdAndCompanyId(Long zoneId, Long companyId);
    Optional<Warehouse> findByZoneId(Long zoneId);
    List<Warehouse> findAllByCompanyId(Long companyId);
    boolean existsByCompanyIdAndName(Long companyId, String name);
    boolean existsByCompanyIdAndNameAndIdIsNot(Long companyId, String name, Long excludedId);
    void deleteById(Long warehouseId);
}
