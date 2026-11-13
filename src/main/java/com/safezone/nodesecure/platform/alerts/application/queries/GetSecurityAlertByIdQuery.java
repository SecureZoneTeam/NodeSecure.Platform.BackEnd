package com.safezone.nodesecure.platform.alerts.application.queries;

/**
 * Query for retrieving a security alert by its identifier.
 *
 * @param id alert identifier
 */
public record GetSecurityAlertByIdQuery(Long id) {
    public GetSecurityAlertByIdQuery {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("alert.error.id.invalid");
        }
    }
}
