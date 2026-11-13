package com.safezone.nodesecure.platform.alerts.application.commandservices;

import com.safezone.nodesecure.platform.alerts.application.commands.MarkAlertAsFalseAlarmCommand;
import com.safezone.nodesecure.platform.alerts.application.commands.RaiseSecurityAlertCommand;
import com.safezone.nodesecure.platform.alerts.application.commands.ReconcileAlertCommand;
import com.safezone.nodesecure.platform.alerts.domain.model.aggregates.SecurityAlert;
import com.safezone.nodesecure.platform.shared.application.result.Result;

/**
 * Application service contract for security alert command operations.
 *
 * @since 1.0
 */
public interface SecurityAlertCommandService {
    /**
     * Raises a new open alert.
     *
     * @param command raise command
     * @return the created alert
     * @throws IllegalArgumentException if the message or ids violate the domain rules
     */
    Result<SecurityAlert, SecurityAlertCommandFailure> handle(RaiseSecurityAlertCommand command);

    /**
     * Closes an open alert as a false alarm; the justification is mandatory (US07).
     *
     * @param command false alarm command
     * @return the closed alert, or failure when it does not exist or is not open
     * @throws IllegalArgumentException if the justification or the person violate the domain rules
     */
    Result<SecurityAlert, SecurityAlertCommandFailure> handle(MarkAlertAsFalseAlarmCommand command);

    /**
     * Closes an open alert as reconciled.
     *
     * @param command reconcile command
     * @return the closed alert, or failure when it does not exist or is not open
     * @throws IllegalArgumentException if the justification or the person violate the domain rules
     */
    Result<SecurityAlert, SecurityAlertCommandFailure> handle(ReconcileAlertCommand command);
}
