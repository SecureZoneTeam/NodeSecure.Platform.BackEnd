package com.safezone.nodesecure.platform.alerts.interfaces.rest.transform;

import com.safezone.nodesecure.platform.alerts.application.commandservices.SecurityAlertCommandFailure;
import com.safezone.nodesecure.platform.alerts.domain.model.aggregates.SecurityAlert;
import com.safezone.nodesecure.platform.shared.application.result.Result;
import com.safezone.nodesecure.platform.shared.interfaces.rest.transform.ProblemResponseMapper;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Interface layer translator converting a security alert command result into an HTTP ResponseEntity.
 *
 * @since 1.0
 */
public class SecurityAlertCommandHttpResponseMapper {
    /**
     * @param result        result of the alert command execution
     * @param messageSource source for localized response messages
     * @param successStatus status used when the command succeeded (201 raise, 200 close)
     * @return the alert resource, or a problem detail (404 not found, 409 already resolved)
     */
    public static ResponseEntity<?> toResponseEntityFromResult(
            Result<SecurityAlert, SecurityAlertCommandFailure> result,
            MessageSource messageSource,
            HttpStatus successStatus) {
        return result.fold(
                alert -> new ResponseEntity<>(SecurityAlertResourceMapper.toResourceFromEntity(alert), successStatus),
                failure -> ProblemResponseMapper.problemDetail(statusFromFailure(failure), messageSource,
                        failure.messageKey(), failure.messageArgs()));
    }

    private static HttpStatus statusFromFailure(SecurityAlertCommandFailure failure) {
        if (failure instanceof SecurityAlertCommandFailure.NotFound) {
            return HttpStatus.NOT_FOUND;
        }
        if (failure instanceof SecurityAlertCommandFailure.AlreadyResolved) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
