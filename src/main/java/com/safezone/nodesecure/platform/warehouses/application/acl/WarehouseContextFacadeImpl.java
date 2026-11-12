package com.safezone.nodesecure.platform.warehouses.application.acl;

import com.safezone.nodesecure.platform.warehouses.domain.repositories.WarehouseRepository;
import com.safezone.nodesecure.platform.warehouses.interfaces.acl.WarehouseContextFacade;
import com.safezone.nodesecure.platform.warehouses.interfaces.acl.WarehouseZoneConfiguration;
import com.safezone.nodesecure.platform.warehouses.domain.model.aggregates.Warehouse;
import com.safezone.nodesecure.platform.warehouses.domain.model.entities.WarehouseZone;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WarehouseContextFacadeImpl implements WarehouseContextFacade {

    private final WarehouseRepository repository;

    public WarehouseContextFacadeImpl(WarehouseRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean zoneBelongsToCompany(Long zoneId, Long companyId) {
        return findZoneConfiguration(zoneId, companyId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WarehouseZoneConfiguration> findZoneConfiguration(Long zoneId, Long companyId) {
        if (zoneId == null || companyId == null) return Optional.empty();
        return repository.findByZoneIdAndCompanyId(zoneId, companyId).flatMap(warehouse ->
                warehouse.getZones().stream().filter(zone -> zone.getId().equals(zoneId))
                        .findFirst().map(zone -> toConfiguration(warehouse, zone)));
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> findZoneIdsByWarehouseId(Long warehouseId, Long companyId) {
        if (warehouseId == null || companyId == null) return Set.of();
        return repository.findByIdAndCompanyId(warehouseId, companyId)
                .map(warehouse -> warehouse.getZones().stream()
                        .map(WarehouseZone::getId)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toUnmodifiableSet()))
                .orElseGet(Set::of);
    }

    private WarehouseZoneConfiguration toConfiguration(Warehouse warehouse, WarehouseZone zone) {
        return new WarehouseZoneConfiguration(warehouse.getId(), zone.getId(), warehouse.getCompanyId(),
                warehouse.getStatus(), zone.getRiskLevel(), zone.getOperatingShift().startText(),
                zone.getOperatingShift().endText());
    }
}
