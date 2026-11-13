package com.safezone.nodesecure.platform.inventory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Response resource representing a product")
public record ProductResource(
        @Schema(description = "Persistent identifier", example = "1")
        Long id,
        @Schema(description = "Company that owns the product", example = "1")
        Long companyId,
        @Schema(description = "Product name", example = "Cable UTP Cat6 305m")
        String name,
        @Schema(description = "Product SKU", example = "NET-UTP6-305")
        String sku,
        @Schema(description = "Product category", example = "NETWORKING")
        String category,
        @Schema(description = "Unit of measure", example = "UNIT")
        String unit,
        @Schema(description = "Units currently in stock", example = "42")
        int currentStock,
        @Schema(description = "Creation instant (UTC)", example = "2026-10-01T09:00:00Z")
        Instant createdAt,
        @Schema(description = "Last update instant (UTC)", example = "2026-10-30T11:20:00Z")
        Instant updatedAt,
        @Schema(description = "Username of the creator", example = "fperez")
        String createdBy) {
}
