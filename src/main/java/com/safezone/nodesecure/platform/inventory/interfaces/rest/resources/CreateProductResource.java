package com.safezone.nodesecure.platform.inventory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Request resource used to create a product. The stock starts at zero.")
public record CreateProductResource(
        @Schema(description = "Company that owns the product", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "{product.error.companyId.invalid}")
        @Positive(message = "{product.error.companyId.invalid}")
        Long companyId,

        @Schema(description = "Product name", example = "Router Wi-Fi 6 AX1800", minLength = 3, maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "{product.error.name.notBlank}")
        @Size(min = 3, max = 100, message = "{product.error.name.size}")
        String name,

        @Schema(description = "Product SKU: letters and digits separated by hyphens (stored in uppercase)",
                example = "NET-ROUTER-AX1800", minLength = 3, maxLength = 32,
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
        String unit,

        @Schema(description = "Username of the person creating the product", example = "lravello", maxLength = 100)
        @Size(max = 100, message = "{product.error.createdBy.size}")
        String createdBy) {
}
