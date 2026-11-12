package com.safezone.nodesecure.platform.warehouses.interfaces.acl;

import java.util.Optional;
import java.util.Set;

/** Public contract used by other bounded contexts without exposing warehouse repositories. */
public interface WarehouseContextFacade {
    boolean zoneBelongsToCompany(Long zoneId, Long companyId);
    Optional<WarehouseZoneConfiguration> findZoneConfiguration(Long zoneId, Long companyId);
    Set<Long> findZoneIdsByWarehouseId(Long warehouseId, Long companyId);
}
