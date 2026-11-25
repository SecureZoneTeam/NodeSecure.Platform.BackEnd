package com.safezone.nodesecure.platform.inventory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request resource used to register a stock movement. The product and the warehouse must belong to the
 * organization of the session and the warehouse must be assigned to the user. {@code performedBy} is
 * accepted for compatibility but ignored: the username of the session is recorded.
 */
@Schema(description = "Request resource used to register a stock movement. "
        + "For RECONCILIATION, quantity is the physically counted stock and justification is mandatory (10-300 characters).")
public record RegisterStockMovementResource(
        @Schema(description = "Product of the organization", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{stock.error.product.invalid}")
        @Positive(message = "{stock.error.product.invalid}")
        Long productId,
        @Schema(description = "Warehouse assigned to the user", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{stock.error.warehouse.invalid}")
        @Positive(message = "{stock.error.warehouse.invalid}")
        Long warehouseId,
        @Schema(description = "Kind of movement", example = "EXIT",
                allowableValues = {"ENTRY", "EXIT", "RECONCILIATION"}, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{stock.error.type.required}")
        String type,
        @Schema(description = "Units moved, or counted stock for RECONCILIATION", example = "8",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{stock.error.quantity.invalid}")
        @Min(value = 0, message = "{stock.error.quantity.invalid}")
        Integer quantity,
        @Schema(description = "Ignored: the username of the session is used", nullable = true)
        String performedBy,
        @Schema(description = "Reason, mandatory for RECONCILIATION",
                example = "Physical count after the unauthorized access alert", maxLength = 300)
        @Size(max = 300, message = "{stock.error.justification.invalid}")
        String justification) {
}
