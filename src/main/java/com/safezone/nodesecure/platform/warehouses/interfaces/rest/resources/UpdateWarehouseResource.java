package com.safezone.nodesecure.platform.warehouses.interfaces.rest.resources;

import com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects.WarehouseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateWarehouseResource(
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Size(max = 160) String street,
        @NotBlank @Size(max = 100) String district,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(min = 2, max = 2) String country,
        @NotNull WarehouseStatus status) {
}
