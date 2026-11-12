package com.safezone.nodesecure.platform.sensors.infrastructure.persistence.jpa.mappers;

import com.safezone.nodesecure.platform.sensors.domain.model.aggregates.SensorNode;
import com.safezone.nodesecure.platform.sensors.domain.model.valueobjects.MacAddress;
import com.safezone.nodesecure.platform.sensors.infrastructure.persistence.jpa.entities.SensorNodeJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class SensorNodePersistenceMapper {
    public SensorNodeJpaEntity toEntity(SensorNode node) {
        SensorNodeJpaEntity entity = new SensorNodeJpaEntity();
        entity.setId(node.getId());
        entity.setCompanyId(node.getCompanyId());
        entity.setZoneId(node.getZoneId());
        entity.setMacAddress(node.getMacAddress().value());
        entity.setModel(node.getModel());
        entity.setSensorType(node.getSensorType());
        entity.setConnectionStatus(node.getConnectionStatus());
        entity.setLastPingAt(node.getLastPingAt());
        entity.setCreatedAt(node.getCreatedAt());
        entity.setUpdatedAt(node.getUpdatedAt());
        return entity;
    }

    public SensorNode toDomain(SensorNodeJpaEntity entity) {
        return SensorNode.restore(entity.getId(), entity.getCompanyId(), entity.getZoneId(),
                new MacAddress(entity.getMacAddress()), entity.getModel(), entity.getSensorType(),
                entity.getConnectionStatus(), entity.getLastPingAt(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
