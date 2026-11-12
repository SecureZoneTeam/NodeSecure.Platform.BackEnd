package com.safezone.nodesecure.platform.warehouses.infrastructure.persistence.jpa.mappers;

import com.safezone.nodesecure.platform.warehouses.domain.model.aggregates.Warehouse;
import com.safezone.nodesecure.platform.warehouses.domain.model.entities.WarehouseZone;
import com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects.OperatingShift;
import com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects.StreetAddress;
import com.safezone.nodesecure.platform.warehouses.infrastructure.persistence.jpa.entities.WarehouseJpaEntity;
import com.safezone.nodesecure.platform.warehouses.infrastructure.persistence.jpa.entities.WarehouseZoneJpaEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Maps persistence entities to pure domain aggregates and back. */
@Component
public class WarehousePersistenceMapper {

    public WarehouseJpaEntity toEntity(Warehouse warehouse) {
        WarehouseJpaEntity entity = new WarehouseJpaEntity();
        entity.setId(warehouse.getId());
        entity.setCompanyId(warehouse.getCompanyId());
        entity.setName(warehouse.getName());
        entity.setStreet(warehouse.getAddress().street());
        entity.setDistrict(warehouse.getAddress().district());
        entity.setCity(warehouse.getAddress().city());
        entity.setCountry(warehouse.getAddress().country());
        entity.setStatus(warehouse.getStatus());
        entity.setCreatedAt(warehouse.getCreatedAt());
        entity.setUpdatedAt(warehouse.getUpdatedAt());

        List<WarehouseZoneJpaEntity> zones = new ArrayList<>();
        for (WarehouseZone zone : warehouse.getZones()) {
            WarehouseZoneJpaEntity zoneEntity = new WarehouseZoneJpaEntity();
            zoneEntity.setId(zone.getId());
            zoneEntity.setWarehouse(entity);
            zoneEntity.setName(zone.getName());
            zoneEntity.setCode(zone.getCode());
            zoneEntity.setRiskLevel(zone.getRiskLevel());
            zoneEntity.setOpeningShiftStart(zone.getOperatingShift().startText());
            zoneEntity.setOpeningShiftEnd(zone.getOperatingShift().endText());
            zones.add(zoneEntity);
        }
        entity.replaceZones(zones);
        return entity;
    }

    public Warehouse toDomain(WarehouseJpaEntity entity) {
        List<WarehouseZone> zones = entity.getZones().stream()
                .map(zone -> WarehouseZone.restore(zone.getId(), entity.getId(), zone.getName(), zone.getCode(),
                        zone.getRiskLevel(), OperatingShift.from(zone.getOpeningShiftStart(), zone.getOpeningShiftEnd())))
                .toList();
        return Warehouse.restore(entity.getId(), entity.getCompanyId(), entity.getName(),
                new StreetAddress(entity.getStreet(), entity.getDistrict(), entity.getCity(), entity.getCountry()),
                entity.getStatus(), zones, entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
