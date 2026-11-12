package com.safezone.nodesecure.platform.sensors.infrastructure.persistence.jpa.mappers;

import com.safezone.nodesecure.platform.sensors.domain.model.aggregates.PhysicalEvent;
import com.safezone.nodesecure.platform.sensors.infrastructure.persistence.jpa.entities.PhysicalEventJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PhysicalEventPersistenceMapper {
    public PhysicalEventJpaEntity toEntity(PhysicalEvent event) {
        PhysicalEventJpaEntity entity = new PhysicalEventJpaEntity();
        entity.setId(event.getId());
        entity.setCompanyId(event.getCompanyId());
        entity.setSensorNodeId(event.getSensorNodeId());
        entity.setZoneId(event.getZoneId());
        entity.setEventType(event.getEventType());
        entity.setOccurredAt(event.getOccurredAt());
        entity.setReconciled(event.isReconciled());
        return entity;
    }

    public PhysicalEvent toDomain(PhysicalEventJpaEntity entity) {
        return PhysicalEvent.restore(entity.getId(), entity.getCompanyId(), entity.getSensorNodeId(),
                entity.getZoneId(), entity.getEventType(), entity.getOccurredAt(), entity.isReconciled());
    }
}
