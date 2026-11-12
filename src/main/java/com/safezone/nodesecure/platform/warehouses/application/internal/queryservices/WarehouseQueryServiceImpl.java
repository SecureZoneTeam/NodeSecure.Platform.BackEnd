package com.safezone.nodesecure.platform.warehouses.application.internal.queryservices;

import com.safezone.nodesecure.platform.warehouses.application.queries.*;
import com.safezone.nodesecure.platform.warehouses.application.queryservices.WarehouseQueryService;
import com.safezone.nodesecure.platform.warehouses.domain.model.aggregates.Warehouse;
import com.safezone.nodesecure.platform.warehouses.domain.model.entities.WarehouseZone;
import com.safezone.nodesecure.platform.warehouses.domain.repositories.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class WarehouseQueryServiceImpl implements WarehouseQueryService {

    private final WarehouseRepository repository;

    public WarehouseQueryServiceImpl(WarehouseRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Warehouse> handle(GetAllWarehousesQuery query) {
        return repository.findAllByCompanyId(query.companyId()).stream()
                .filter(warehouse -> query.warehouseId() == null
                        || warehouse.getId().equals(query.warehouseId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Warehouse> handle(GetWarehouseByIdQuery query) {
        if (query.warehouseId() != null && !query.warehouseId().equals(query.id())) {
            return Optional.empty();
        }
        return repository.findByIdAndCompanyId(query.id(), query.companyId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseZone> handle(GetAllWarehouseZonesQuery query) {
        return repository.findAllByCompanyId(query.companyId()).stream()
                .filter(warehouse -> query.warehouseId() == null
                        || warehouse.getId().equals(query.warehouseId()))
                .flatMap(warehouse -> warehouse.getZones().stream()).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WarehouseZone> handle(GetWarehouseZoneByIdQuery query) {
        return repository.findByZoneIdAndCompanyId(query.zoneId(), query.companyId())
                .filter(warehouse -> query.warehouseId() == null
                        || warehouse.getId().equals(query.warehouseId()))
                .flatMap(warehouse -> warehouse.getZones().stream()
                        .filter(zone -> zone.getId().equals(query.zoneId())).findFirst());
    }
}
