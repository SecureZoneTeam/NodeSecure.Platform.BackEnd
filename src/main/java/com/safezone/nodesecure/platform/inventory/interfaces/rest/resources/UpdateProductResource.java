package com.safezone.nodesecure.platform.inventory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request resource used to update a product. Stock cannot be edited, only moved with stock movements.")
public record UpdateProductResource(
        @Schema(description = "Product name", example = "Router Wi-Fi 6 AX1800", minLength = 3, maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{product.error.name.notBlank}")
        @Size(min = 3, max = 100, message = "{product.error.name.size}")
        String name,

        @Schema(description = "Product SKU", example = "NET-ROUTER-AX1800", minLength = 3, maxLength = 32,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{product.error.sku.invalid}")
        @Size(min = 3, max = 32, message = "{product.error.sku.invalid}")
        @Pattern(regexp = "^[A-Za-z0-9]+(-[A-Za-z0-9]+)*$", message = "{product.error.sku.invalid}")
        String sku,

        @Schema(description = "Product category", example = "NETWORKING",
                allowableValues = {"NETWORKING", "SECURITY", "COMPUTING", "ELECTRONICS", "ACCESSORIES", "OTHER"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{product.error.category.required}")
        String category,

        @Schema(description = "Unit of measure (UNIT when omitted)", example = "UNIT", maxLength = 20)
        @Size(max = 20, message = "{product.error.unit.size}")
        String unit) {
}
