package com.safezone.nodesecure.platform.warehouses.interfaces.acl;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/** Public contract used by other bounded contexts without exposing warehouse repositories. */
public interface WarehouseContextFacade {
    boolean zoneBelongsToCompany(Long zoneId, Long companyId);
    Optional<WarehouseZoneConfiguration> findZoneConfiguration(Long zoneId, Long companyId);
    Set<Long> findZoneIdsByWarehouseId(Long warehouseId, Long companyId);

    /** Zones of the given warehouses that belong to the company ({@code null} warehouses = every warehouse). */
    Set<Long> findZoneIdsByWarehouseIds(Collection<Long> warehouseIds, Long companyId);

    boolean warehouseBelongsToCompany(Long warehouseId, Long companyId);

    /** Ids of every warehouse of the company. */
    Set<Long> findWarehouseIdsByCompanyId(Long companyId);

    /** True only when every id exists and belongs to the company. */
    boolean allWarehousesBelongToCompany(Collection<Long> warehouseIds, Long companyId);
}
