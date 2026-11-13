package com.safezone.nodesecure.platform.alerts.infrastructure.persistence.jpa.mappers;

import com.safezone.nodesecure.platform.alerts.domain.model.aggregates.SecurityAlert;
import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertSeverity;
import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertStatus;
import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertType;
import com.safezone.nodesecure.platform.alerts.infrastructure.persistence.jpa.entities.SecurityAlertJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class SecurityAlertPersistenceMapper {
    public SecurityAlertJpaEntity toEntity(SecurityAlert alert) {
        return new SecurityAlertJpaEntity(alert.getId(), alert.getWarehouseId(), alert.getZoneId(),
                alert.getPhysicalEventId(), alert.getAlertType().name(), alert.getSeverity().name(),
                alert.getStatus().name(), alert.getMessage(), alert.getRaisedAt(), alert.getJustification(),
                alert.getResolvedAt(), alert.getResolvedBy());
    }

    public SecurityAlert toDomain(SecurityAlertJpaEntity entity) {
        return SecurityAlert.rehydrate(entity.getId(), entity.getWarehouseId(), entity.getZoneId(),
                entity.getPhysicalEventId(), AlertType.valueOf(entity.getAlertType()),
                AlertSeverity.valueOf(entity.getSeverity()), AlertStatus.valueOf(entity.getStatus()),
                entity.getMessage(), entity.getRaisedAt(), entity.getJustification(), entity.getResolvedAt(),
                entity.getResolvedBy());
    }
}
