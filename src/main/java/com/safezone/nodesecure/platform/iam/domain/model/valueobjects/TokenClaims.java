package com.safezone.nodesecure.platform.iam.domain.model.valueobjects;

/**
 * Identity carried by the JWT. It deliberately contains no role, permission or warehouse:
 * those are read from the database on every request so that changes take effect immediately.
 */
public record TokenClaims(Long userId, String username, Long companyId, long tokenVersion) {
}
