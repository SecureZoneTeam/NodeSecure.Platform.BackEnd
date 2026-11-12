package com.safezone.nodesecure.platform.alerts.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Response resource representing a security alert.
 * Field names match the security-alerts mock data of the Angular front end.
 */
@Schema(description = "Response resource representing a security alert")
public record SecurityAlertResource(
        @Schema(description = "Persistent identifier", example = "1")
        Long id,
        @Schema(description = "Warehouse where the alert happened", example = "1")
        Long warehouseId,
        @Schema(description = "Zone where the alert happened", example = "3", nullable = true)
        Long zoneId,
        @Schema(description = "Physical event that originated the alert", example = "2", nullable = true)
        Long physicalEventId,
        @Schema(description = "Kind of alert", example = "UNAUTHORIZED_ACCESS",
                allowableValues = {"DISCREPANCY", "UNAUTHORIZED_ACCESS", "NODE_DISCONNECTION"})
        String alertType,
        @Schema(description = "Severity", example = "CRITICAL", allowableValues = {"MINOR", "MAJOR", "CRITICAL"})
        String severity,
        @Schema(description = "Lifecycle status", example = "OPEN", allowableValues = {"OPEN", "RECONCILED", "FALSE_ALARM"})
        String status,
        @Schema(description = "Description of the alert", example = "Unauthorized access detected outside scheduled operating hours.")
        String message,
        @Schema(description = "Instant the alert was raised (UTC)", example = "2026-10-04T02:47:31Z")
        Instant raisedAt,
        @Schema(description = "Justification given when the alert was closed", nullable = true)
        String justification,
        @Schema(description = "Instant the alert was closed (UTC)", nullable = true)
        Instant resolvedAt,
        @Schema(description = "Username of the person who closed the alert", nullable = true)
        String resolvedBy) {
}
