package com.safezone.nodesecure.platform.alerts.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request resource used to close an alert as a false alarm (US07). The justification is mandatory.
 */
@Schema(description = "Request resource used to mark an alert as a false alarm. The justification is mandatory.")
public record MarkAlertAsFalseAlarmResource(
        @Schema(description = "Why the alert is a false alarm", example = "Movimiento de stock autorizado verbalmente por el supervisor.",
                maxLength = 300, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{alert.error.justification.required}")
        @Size(max = 300, message = "{alert.error.justification.size}")
        String justification,

        @Schema(description = "Username of the person closing the alert", example = "lanahua", maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{alert.error.resolvedBy.notBlank}")
        @Size(max = 100, message = "{alert.error.resolvedBy.size}")
        String resolvedBy) {
}
