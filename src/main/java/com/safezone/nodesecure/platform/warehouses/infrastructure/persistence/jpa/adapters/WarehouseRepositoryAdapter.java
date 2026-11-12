package com.safezone.nodesecure.platform.warehouses.infrastructure.persistence.jpa.adapters;

import com.safezone.nodesecure.platform.warehouses.domain.model.aggregates.Warehouse;
import com.safezone.nodesecure.platform.warehouses.domain.repositories.WarehouseRepository;
import com.safezone.nodesecure.platform.warehouses.infrastructure.persistence.jpa.mappers.WarehousePersistenceMapper;
import com.safezone.nodesecure.platform.warehouses.infrastructure.persistence.jpa.repositories.WarehouseJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class WarehouseRepositoryAdapter implements WarehouseRepository {

    private final WarehouseJpaRepository jpa;
    private final WarehousePersistenceMapper mapper;

    public WarehouseRepositoryAdapter(WarehouseJpaRepository jpa, WarehousePersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Warehouse save(Warehouse warehouse) {
        return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(warehouse)));
    }

    @Override
    public Optional<Warehouse> findByIdAndCompanyId(Long warehouseId, Long companyId) {
        return jpa.findByIdAndCompanyId(warehouseId, companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<Warehouse> findByZoneIdAndCompanyId(Long zoneId, Long companyId) {
        return jpa.findByCompanyIdAndZones_Id(companyId, zoneId).map(mapper::toDomain);
    }

    @Override
    public Optional<Warehouse> findByZoneId(Long zoneId) {
        return jpa.findByZones_Id(zoneId).map(mapper::toDomain);
    }

    @Override
    public List<Warehouse> findAllByCompanyId(Long companyId) {
        return jpa.findAllByCompanyIdOrderByIdAsc(companyId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByCompanyIdAndName(Long companyId, String name) {
        return jpa.existsByCompanyIdAndNameIgnoreCase(companyId, name);
    }

    @Override
    public boolean existsByCompanyIdAndNameAndIdIsNot(Long companyId, String name, Long excludedId) {
        return jpa.existsByCompanyIdAndNameIgnoreCaseAndIdIsNot(companyId, name, excludedId);
    }

    @Override
    public void deleteById(Long warehouseId) {
        jpa.deleteById(warehouseId);
    }
}
