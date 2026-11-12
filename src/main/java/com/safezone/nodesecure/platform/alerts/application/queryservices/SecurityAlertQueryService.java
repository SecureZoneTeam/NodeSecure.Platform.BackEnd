package com.safezone.nodesecure.platform.alerts.application.queryservices;

import com.safezone.nodesecure.platform.alerts.application.queries.GetAllSecurityAlertsQuery;
import com.safezone.nodesecure.platform.alerts.application.queries.GetSecurityAlertByIdQuery;
import com.safezone.nodesecure.platform.alerts.domain.model.aggregates.SecurityAlert;

import java.util.List;
import java.util.Optional;

/**
 * Application service contract providing read access to security alerts.
 *
 * @since 1.0
 */
public interface SecurityAlertQueryService {
    /**
     * @param query query with optional status, severity and warehouse filters
     * @return alerts, newest first, possibly empty
     */
    List<SecurityAlert> handle(GetAllSecurityAlertsQuery query);

    /**
     * @param query query containing the alert identifier
     * @return alert when found, otherwise empty
     */
    Optional<SecurityAlert> handle(GetSecurityAlertByIdQuery query);
}
