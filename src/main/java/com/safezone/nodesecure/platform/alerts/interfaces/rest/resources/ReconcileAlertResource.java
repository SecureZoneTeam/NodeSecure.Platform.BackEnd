package com.safezone.nodesecure.platform.alerts.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request resource used to close an alert as reconciled.
 */
@Schema(description = "Request resource used to mark an alert as reconciled. The justification is optional.")
public record ReconcileAlertResource(
        @Schema(description = "Optional explanation", example = "Authorized warehouse activity confirmed by the supervisor.",
                maxLength = 300)
        @Size(max = 300, message = "{alert.error.justification.size}")
        String justification,

        @Schema(description = "Username of the person closing the alert", example = "lanahua", maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{alert.error.resolvedBy.notBlank}")
        @Size(max = 100, message = "{alert.error.resolvedBy.size}")
        String resolvedBy) {
}
