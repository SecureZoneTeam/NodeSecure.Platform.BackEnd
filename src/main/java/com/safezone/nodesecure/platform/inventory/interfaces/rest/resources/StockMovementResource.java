package com.safezone.nodesecure.platform.inventory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Response resource representing an immutable stock movement")
public record StockMovementResource(
        @Schema(description = "Persistent identifier", example = "2")
        Long id,
        @Schema(description = "Affected product", example = "1")
        Long productId,
        @Schema(description = "Warehouse where the movement happened", example = "1")
        Long warehouseId,
        @Schema(description = "Kind of movement", example = "EXIT", allowableValues = {"ENTRY", "EXIT", "RECONCILIATION"})
        String type,
        @Schema(description = "Units moved (counted stock for RECONCILIATION)", example = "8")
        int quantity,
        @Schema(description = "Stock before the movement", example = "50")
        int previousStock,
        @Schema(description = "Stock after the movement", example = "42")
        int resultingStock,
        @Schema(description = "Username of the person who performed the movement", example = "ksandoval")
        String performedBy,
        @Schema(description = "Instant of the movement (UTC)", example = "2026-10-30T11:20:00Z")
        Instant occurredAt,
        @Schema(description = "Reason of a RECONCILIATION, null otherwise", nullable = true)
        String justification) {
}
