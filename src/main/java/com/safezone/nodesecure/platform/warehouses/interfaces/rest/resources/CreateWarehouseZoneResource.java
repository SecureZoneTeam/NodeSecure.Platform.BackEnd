package com.safezone.nodesecure.platform.warehouses.interfaces.rest.resources;

import com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects.ZoneRiskLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateWarehouseZoneResource(
        @NotNull @Positive Long warehouseId,
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Size(max = 30) String code,
        @NotNull ZoneRiskLevel riskLevel,
        @NotBlank @Pattern(regexp = "^(?:[01]\\d|2[0-3]):[0-5]\\d$", message = "{warehouse.error.zone.shift.invalid}") String openingShiftStart,
        @NotBlank @Pattern(regexp = "^(?:[01]\\d|2[0-3]):[0-5]\\d$", message = "{warehouse.error.zone.shift.invalid}") String openingShiftEnd) {
}
