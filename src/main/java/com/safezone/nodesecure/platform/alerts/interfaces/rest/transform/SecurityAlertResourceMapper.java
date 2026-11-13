package com.safezone.nodesecure.platform.alerts.interfaces.rest.transform;

import com.safezone.nodesecure.platform.alerts.domain.model.aggregates.SecurityAlert;
import com.safezone.nodesecure.platform.alerts.interfaces.rest.resources.SecurityAlertResource;

/**
 * Interface layer translator converting the {@link SecurityAlert} aggregate to its HTTP resource.
 *
 * @since 1.0
 */
public class SecurityAlertResourceMapper {
    public static SecurityAlertResource toResourceFromEntity(SecurityAlert entity) {
        return new SecurityAlertResource(
                entity.getId(),
                entity.getWarehouseId(),
                entity.getZoneId(),
                entity.getPhysicalEventId(),
                entity.getAlertType().name(),
                entity.getSeverity().name(),
                entity.getStatus().name(),
                entity.getMessage(),
                entity.getRaisedAt(),
                entity.getJustification(),
                entity.getResolvedAt(),
                entity.getResolvedBy());
    }
}
