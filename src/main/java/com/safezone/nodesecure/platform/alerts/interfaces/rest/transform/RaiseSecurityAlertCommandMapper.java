package com.safezone.nodesecure.platform.alerts.interfaces.rest.transform;

import com.safezone.nodesecure.platform.alerts.application.commands.RaiseSecurityAlertCommand;
import com.safezone.nodesecure.platform.alerts.interfaces.rest.resources.RaiseSecurityAlertResource;

/**
 * Interface layer translator converting {@link RaiseSecurityAlertResource} to {@link RaiseSecurityAlertCommand}.
 *
 * @since 1.0
 */
public class RaiseSecurityAlertCommandMapper {
    public static RaiseSecurityAlertCommand toCommandFromResource(RaiseSecurityAlertResource resource) {
        return new RaiseSecurityAlertCommand(
                resource.warehouseId(),
                resource.zoneId(),
                resource.physicalEventId(),
                AlertValueObjectMapper.toAlertTypeFromString(resource.alertType()),
                AlertValueObjectMapper.toOptionalSeverityFromString(resource.severity()),
                resource.message());
    }
}
