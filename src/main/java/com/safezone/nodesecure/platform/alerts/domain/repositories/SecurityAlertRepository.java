package com.safezone.nodesecure.platform.alerts.domain.repositories;

import com.safezone.nodesecure.platform.alerts.domain.model.aggregates.SecurityAlert;
import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertSeverity;
import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertStatus;

import java.util.List;
import java.util.Optional;

/** Technology-independent outbound persistence port for security alerts. */
public interface SecurityAlertRepository {
    SecurityAlert save(SecurityAlert alert);

    Optional<SecurityAlert> findById(Long id);

    /** Finds an alert locking its row, so two people cannot close the same alert at once. */
    Optional<SecurityAlert> findByIdForUpdate(Long id);

    /**
     * Lists alerts, newest first. Every filter is optional ({@code null} means "any").
     */
    List<SecurityAlert> findAll(AlertStatus status, AlertSeverity severity, Long warehouseId);
}
