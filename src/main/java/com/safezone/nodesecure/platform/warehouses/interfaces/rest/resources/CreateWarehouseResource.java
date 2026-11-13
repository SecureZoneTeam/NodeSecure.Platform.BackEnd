package com.safezone.nodesecure.platform.warehouses.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "CreateWarehouseRequest", description = "Data required to register a warehouse")
public record CreateWarehouseResource(
        @NotBlank @Size(max = 80) @Schema(example = "Ate Logistics Warehouse") String name,
        @NotBlank @Size(max = 160) @Schema(example = "Av. Separadora Industrial 1850") String street,
        @NotBlank @Size(max = 100) @Schema(example = "Ate") String district,
        @NotBlank @Size(max = 100) @Schema(example = "Lima") String city,
        @NotBlank @Size(min = 2, max = 2) @Schema(example = "PE") String country) {
}
