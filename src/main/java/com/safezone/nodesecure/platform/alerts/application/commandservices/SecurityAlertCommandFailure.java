package com.safezone.nodesecure.platform.alerts.application.commandservices;

import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertStatus;

/**
 * Failure types for security alert command execution.
 */
public sealed interface SecurityAlertCommandFailure
        permits SecurityAlertCommandFailure.NotFound, SecurityAlertCommandFailure.AlreadyResolved {
    /**
     * @return message key associated with the failure
     */
    String messageKey();

    /**
     * @return values used to fill the localized message
     */
    default Object[] messageArgs() {
        return new Object[0];
    }

    /** The alert does not exist. */
    record NotFound(Long id) implements SecurityAlertCommandFailure {
        @Override
        public String messageKey() {
            return "alert.error.notFoundById";
        }

        @Override
        public Object[] messageArgs() {
            return new Object[]{id};
        }
    }

    /** The alert was already closed as reconciled or as a false alarm. */
    record AlreadyResolved(Long id, AlertStatus status) implements SecurityAlertCommandFailure {
        @Override
        public String messageKey() {
            return "alert.error.alreadyResolved";
        }

        @Override
        public Object[] messageArgs() {
            return new Object[]{id, status};
        }
    }
}
