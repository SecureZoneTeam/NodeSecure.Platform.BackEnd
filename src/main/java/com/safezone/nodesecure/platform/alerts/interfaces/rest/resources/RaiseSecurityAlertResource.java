package com.safezone.nodesecure.platform.alerts.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request resource used to raise a security alert (US03, US05).
 */
@Schema(description = "Request resource used to raise a security alert. The alert is created OPEN.")
public record RaiseSecurityAlertResource(
        @Schema(description = "Warehouse where the anomaly happened", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{alert.error.warehouseId.invalid}")
        @Positive(message = "{alert.error.warehouseId.invalid}")
        Long warehouseId,

        @Schema(description = "Zone where the anomaly happened", example = "3")
        @Positive(message = "{alert.error.zoneId.invalid}")
        Long zoneId,

        @Schema(description = "Physical event that originated the alert", example = "2")
        @Positive(message = "{alert.error.physicalEventId.invalid}")
        Long physicalEventId,

        @Schema(description = "Kind of alert", example = "UNAUTHORIZED_ACCESS",
                allowableValues = {"DISCREPANCY", "UNAUTHORIZED_ACCESS", "NODE_DISCONNECTION"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{alert.error.type.required}")
        String alertType,

        @Schema(description = "Severity. When omitted: UNAUTHORIZED_ACCESS=CRITICAL, NODE_DISCONNECTION=MAJOR, DISCREPANCY=MINOR",
                example = "CRITICAL", allowableValues = {"MINOR", "MAJOR", "CRITICAL"})
        String severity,

        @Schema(description = "Description of the alert", example = "Unauthorized access detected outside scheduled operating hours.",
                maxLength = 255, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{alert.error.message.notBlank}")
        @Size(max = 255, message = "{alert.error.message.size}")
        String message) {
}
