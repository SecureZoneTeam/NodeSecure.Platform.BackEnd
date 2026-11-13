package com.safezone.nodesecure.platform.inventory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Request resource used to register a stock movement. "
        + "For RECONCILIATION, quantity is the physically counted stock and justification is mandatory (10-300 characters).")
public record RegisterStockMovementResource(
        @Schema(description = "Product whose stock changes", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{stock.error.product.invalid}")
        @Positive(message = "{stock.error.product.invalid}")
        Long productId,

        @Schema(description = "Warehouse where the movement happens", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{stock.error.warehouse.invalid}")
        @Positive(message = "{stock.error.warehouse.invalid}")
        Long warehouseId,

        @Schema(description = "Kind of movement", example = "ENTRY",
                allowableValues = {"ENTRY", "EXIT", "RECONCILIATION"}, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{stock.error.type.required}")
        String type,

        @Schema(description = "Units moved (counted stock for RECONCILIATION)", example = "10",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{stock.error.quantity.invalid}")
        @Min(value = 0, message = "{stock.error.quantity.invalid}")
        Integer quantity,

        @Schema(description = "Username of the person who performs the movement", example = "ksandoval", maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{stock.error.performedBy.notBlank}")
        @Size(max = 100, message = "{stock.error.performedBy.size}")
        String performedBy,

        @Schema(description = "Reason of a RECONCILIATION (ignored for ENTRY and EXIT)",
                example = "Physical count after the unauthorized access alert", maxLength = 300)
        @Size(max = 300, message = "{stock.error.justification.invalid}")
        String justification) {
}
